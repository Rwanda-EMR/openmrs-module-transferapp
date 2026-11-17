package org.openmrs.module.transferapp.page.controller.findpatient;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.appframework.domain.AppDescriptor;
import org.openmrs.ui.framework.UiUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindPatientPageControllerTest {

	private UiUtils ui;

	@Before
	public void setUp() {
		ui = mock(UiUtils.class);
		when(ui.contextPath()).thenReturn("openmrs");
		when(ui.thisUrlWithContextPath()).thenReturn("/openmrs/coreapps/findpatient/findPatient.page?app=test.app&");
		when(ui.message("coreapps.findPatient.app.label")).thenReturn("Find Patient Record");
		when(ui.message("Laboratory Orders")).thenReturn("Laboratory Orders");
	}

	private AppDescriptor app(String config) throws Exception {
		AppDescriptor app = new AppDescriptor();
		app.setConfig((ObjectNode) new ObjectMapper().readTree(config));
		return app;
	}

	@Test
	public void breadcrumbsJson_shouldAddContextPathTranslateLabelsAndLinkLastToThisPage() throws Exception {
		String json = FindPatientPageController.breadcrumbsJson(app("{\"breadcrumbs\": ["
				+ "{\"icon\": \"icon-home\", \"link\": \"/index.htm\"},"
				+ "{\"label\": \"Laboratory Orders\", \"link\": \"/pihapps/labs/labOrderList.page\"},"
				+ "{\"label\": \"coreapps.findPatient.app.label\"}]}"), ui);
		assertEquals(new ObjectMapper().readTree("["
				+ "{\"icon\": \"icon-home\", \"link\": \"/openmrs/index.htm\"},"
				+ "{\"link\": \"/openmrs/pihapps/labs/labOrderList.page\", \"label\": \"Laboratory Orders\"},"
				+ "{\"link\": \"/openmrs/coreapps/findpatient/findPatient.page?app=test.app\", \"label\": \"Find Patient Record\"}]"),
				new ObjectMapper().readTree(json));
	}

	@Test
	public void breadcrumbsJson_shouldReturnNullIfAppHasNoBreadcrumbs() throws Exception {
		assertNull(FindPatientPageController.breadcrumbsJson(app("{\"label\": \"x\"}"), ui));
	}
}
