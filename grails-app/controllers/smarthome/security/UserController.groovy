package smarthome.security

import smarthome.security.RegisterService;
import smarthome.security.UserService;
import grails.plugin.springsecurity.annotation.Secured;
import smarthome.automation.DeviceSearchCommand;
import smarthome.automation.DeviceService;
import smarthome.automation.HouseService;
import smarthome.automation.NotificationAccount;
import smarthome.automation.deviceType.TeleInformation;
import smarthome.automation.notification.NotificationAccountEnum;
import smarthome.core.AbstractController
import smarthome.core.ExceptionNavigationHandler
import smarthome.core.QueryUtils;
import smarthome.plugin.NavigableAction
import smarthome.plugin.NavigationEnum
import smarthome.security.ChangePasswordCommand;
import smarthome.security.RegistrationCode;
import smarthome.security.User;
/**
 * Controller gestion utilisateur
 * 
 * @author gregory
 *
 */
@Secured("isAuthenticated()")
class UserController extends AbstractController {

	UserService userService
	RegisterService registerService
	HouseService houseService
	DeviceService deviceService


	/**
	 * Affichage du profil simple (pour la modifcation du user par lui-même)
	 * 
	 * @return
	 */
	@NavigableAction(label = "Mon profil", navigation = NavigationEnum.user, defaultGroup = true, header = NavigableAction.SECUSERNAME)
	def profil() {
		// plugin spring security add authenticatedUser property
		def user = parseFlashCommand("user", authenticatedUser)
		def smsAccount = NotificationAccount.findByUserAndType(user, NotificationAccountEnum.sms)
		def house = houseService.findDefaultByUser(user)
		def compteurs = deviceService.listByUser(new DeviceSearchCommand([userId: user.id, 
			deviceTypeClass: TeleInformation.name, sharedDevice: false]))
		render(view: 'profil', model: [user: user, smsAccount: smsAccount, house: house, compteurs: compteurs])
	}
	
	
	/**
	 * Profil publique d'un user
	 * 
	 * @param user
	 * @return
	 */
	def profilPublic(User user) {
		def house = houseService.findDefaultByUser(user)
		def userDeviceCount = deviceService.countDevice(user)
		def sharedDeviceCount = deviceService.listSharedDeviceId(user.id).size()
		render(view: 'profilPublic', model: [user: user, house: house, userDeviceCount: userDeviceCount, 
			sharedDeviceCount: sharedDeviceCount, viewOnly: true])
	}


	/**
	 * Demande changement mot de passe
	 * 
	 * @return
	 */
	def password() {
		// plugin spring security add authenticatedUser property
		def command = parseFlashCommand("command", new ChangePasswordCommand(username: authenticatedUser.username,
		prenom: authenticatedUser.prenom, nom: authenticatedUser.nom))
		render(view: 'password', model: [command: command])
	}


	/**
	 * Enregistrement du profil
	 * 
	 * @param profil
	 * @return
	 */
	@ExceptionNavigationHandler(actionName = "index", modelName = "user")
	def saveProfil(ProfilCommand command) {
		checkErrors(this, command.user)

		// on ne mappe que les infos "non sensibles" (ie pas le mot de passe)
		// @see constraint bindable User
		userService.save(command.user, false)
		
		command.house.user = command.user
		houseService.save(command.house)
		
		profil()
	}


	/**
	 * Confirmation changement mot de passe
	 * 
	 * @return
	 */
	@ExceptionNavigationHandler(actionName = "password", modelName = "command")
	def changePassword(ChangePasswordCommand command) {
		checkErrors(this, command)

		userService.changePassword(command)
		flash.info = "Changement de votre mot de passe effectué avec succès !"
		redirect(action: 'profil')
	}

	/**
	 * Retourne la liste des utilisateurs
	 * 
	 * @return
	 */
	@NavigableAction(label = SmartHomeSecurityConstantes.UTILISATEURS, defaultGroup = true, navigation = NavigationEnum.configuration, header = SmartHomeSecurityConstantes.UTILISATEURS,
	breadcrumb = [
		NavigableAction.CONFIG_APPLICATION,
		SmartHomeSecurityConstantes.UTILISATEUR_SECURITE
	])
	@Secured("hasRole('ROLE_ADMIN')")
	def users(String userSearch) {
		def users
		int recordsTotal
		def pagination = this.getPagination([:])

		if (userSearch) {
			def search = QueryUtils.decorateMatchAll(userSearch)
			
			def query = User.where {
				username =~ search || prenom =~ search || nom =~ search
			}
			users = query.list(pagination)
			recordsTotal = query.count()
		} else {
			recordsTotal = User.count()
			users = User.list(pagination)
		}

		// users est accessible depuis le model avec la variable user[Instance]List
		// @see grails.scaffolding.templates.domainSuffix
		respond users, model: [recordsTotal: recordsTotal, userSearch: userSearch]
	}
	
	
	/**
	 * Recherche utilisateur publique
	 * 
	 * @param userSearch
	 * @return
	 */
	@Secured("isAuthenticated()")
	def userList(String userSearch) {
		users(userSearch)
	}


	/**
	 * Edition d'un utilisateur
	 * 
	 * @param user
	 * @return
	 */
	@Secured("hasRole('ROLE_ADMIN')")
	def edit(User user) {
		def editUser = parseFlashCommand("user", user)
		def userRoles = editUser.getAuthorities()
		def registrations = RegistrationCode.where({
			username == editUser.username
		}).list(sort: 'dateCreated', order: 'desc')
		
		def model = [user: editUser, roles: Role.list(), userRoles: userRoles, 
			registration: registrations ? registrations[0] : null]
		
		render(view: 'user', model: model)
	}


	/**
	 * Création d'un utilisateur
	 *
	 * @param user
	 * @return
	 */
	@Secured("hasRole('ROLE_ADMIN')")
	def create() {
		def editUser = parseFlashCommand("user", new User(lastActivation: new Date(), enabled: false))
		render(view: 'user', model: [user: editUser, roles: Role.list(), userRoles: []])
	}


	/**
	 * Enregistrement d'un utilisateur existant avec toutes ses associations
	 * 
	 * @param user
	 * @return
	 */
	@ExceptionNavigationHandler(actionName = "edit", modelName = "user")
	@Secured("hasRole('ROLE_ADMIN')")
	def saveEdit(User user) {
		checkErrors(this, user)

		userService.save(user, true)
		redirect(action: 'users')
	}


	/**
	 * Enregistrement d'un nouvel utilisateur avec toutes ses associations
	 * 
	 * @param user
	 * @return
	 */
	@ExceptionNavigationHandler(actionName = "create", modelName = "user")
	@Secured("hasRole('ROLE_ADMIN')")
	def saveCreate(User user) {
		userService.save(user, true)
		redirect(action: 'users')
	}
	
	
	/**
	 * Reset d'un mot de passe utilisateur par l'admin
	 * 
	 * @param username
	 * @return
	 */
	@Secured("hasRole('ROLE_ADMIN')")
	def resetPassword(String username) {
		registerService.forgotPassword(username)
		flash.info = "Un email pour réinitialiser le mot de passe a été envoyé à l'adresse suivante : ${username} !"
		redirect(action: 'users')
	}
	
	
	/**
	 * Authentification rapide vers un autre utilisateur
	 *
	 * @param user
	 * @return
	 */
	@Secured("hasRole('ROLE_ADMIN')")
	@ExceptionNavigationHandler(actionName = "users")
	def switchUser(User user) {
		// nettoie la session
		// TODO 
		redirect(uri: "/j_spring_security_switch_user", params: [j_username: user.username])
	}
	
	
	/**
	 * Revenir à la session normale
	 *
	 * @return
	 */
	def exitSwitchUser() {
		// nettoie la session
		// TODO 
		redirect(uri: "/j_spring_security_exit_user")
	}
	
}
