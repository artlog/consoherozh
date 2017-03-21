import grails.util.Environment
import smarthome.core.EndPointUtils

class BootStrap {

	// auto-inject
	def smarthomeScheduler
	
	def init = { servletContext ->
		EndPointUtils.register(servletContext, smarthome.endpoint.AgentEndPoint)
		
		// démarre le gestionnaire de cron
		smarthomeScheduler.start()
	}

	
	def destroy = {
		smarthomeScheduler.shutdown()
	}
}
