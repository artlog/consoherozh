<%@ page import="smarthome.automation.DeviceValue" %>

<g:set var="currentDate" value="${ new Date() }"/>

<h3>Synthèse consommations</h3>

<g:if test="${ house?.compteur }">
	<g:set var="compteurElectrique" value="${ house.compteurElectriqueImpl() }"/>
	<g:set var="consos" value="${ compteurElectrique.consosJour() }"/>
	<g:set var="interpretation" value="${ houseSynthese?.interpretations[house.compteur.id] }"/>
	

	<div class="aui-group">
		<div class="aui-item responsive" style="width: 33.3%">
			
			<div style="margin-top:20px">
				<div class="separator-bottom">
					<div class="aui-group aui-group-split">
						<div class="aui-item" style="width:25%">
							<h4>${ app.formatUser(date: currentDate) }</h4>
						</div>
						<div class="aui-item">
							<h4><span class="link">${ consos.tarifTotal != null ? (consos.tarifTotal as Double).round(1) : '-' }€</span>
								<g:if test="${ params.compare }">
									<g:link class="aui-button" style="float:right; margin-left:10px;" action="compareHouseDeviceChart" controller="device" params="['device.id': house.compteur.id]">Comparer</g:link>
								</g:if>
							</h4>
						</div>
					</div>
				</div>	
				
				<div class="synthese-content">
						
					<g:link controller="device" action="deviceChart" params="['device.id': house.compteur.id]">
						<div class="vignette-synthese" style="background: radial-gradient(#0747a6 ${interpretation?.pourcentage == 100 ? '100%' : ''}, orange ${interpretation?.pourcentage < 100 ? interpretation?.pourcentage + '%' : ''});">
							${ consos.total }kWh
						</div>
					</g:link>
					<h6 class="h6">Dernier relevé : ${ app.formatTimeAgo(date: house.compteur.dateValue) }</h6>
					
					<table class="aui datatable" style="margin-bottom:20px;">
						<thead>
							<tr>
								<th>${ consos.optTarif }</th>
								<th>kWh</th>
								<th>€</th>
							</tr>
						</thead>
						<tbody>
							<g:if test="${ consos.optTarif in ['HC', 'EJP'] }">
								<tr>
									<td>${ consos.optTarif == 'HC' ? 'Heures creuses' : 'Heures normales' }</td>
									<td><span class="link">${ (consos.hchc as Double)?.round(1) }</span></td>
									<td><span class="link">${ (consos.tarifHC as Double)?.round(1) }</span></td>
								</tr>
								<tr>
									<td>${ consos.optTarif == 'HC' ? 'Heures pleines' : 'Heures pointe mobile' }</td>
									<td><span class="link">${ (consos.hchp as Double)?.round(1) }</span></td>
									<td><span class="link">${ (consos.tarifHP as Double)?.round(1) }</span></td>
								</tr>
							</g:if>
							<g:else>
								<tr>
									<td>Toutes heures</td>
									<td><span class="link">${ (consos.base as Double)?.round(1) }</span></td>
									<td><span class="link">${ (consos.tarifBASE as Double)?.round(1) }</span></td>
								</tr>
							</g:else>
						</tbody>
					</table>
					
					<p style="text-align:left"><strong>Interprétation :</strong></p>
				</div>
			</div>
		</div>
		<div class="aui-item responsive">
			<div>
				<g:include action="templateDeviceChart" controller="device" params="[viewMode: 'month',
					dateChart: app.formatPicker(date: new Date()), dateDebutUser: app.formatPicker(date: new Date() - 7),
					'device.id': house.compteur.id, chartHeight: '350']"/>	
			</div>
		</div>
	</div>
</g:if>
<g:else>
	<p class="label">Profil incomplet : les objets par défaut ne sont pas renseignés</p>
</g:else>