package com.stretchcom.rteam.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class MemberDeleteConfirmationError extends Composite {

	private static MemberDeleteConfirmationErrorUiBinder uiBinder = GWT.create(MemberDeleteConfirmationErrorUiBinder.class);

	interface MemberDeleteConfirmationErrorUiBinder extends UiBinder<Widget, MemberDeleteConfirmationError> {}

	@UiField
	SpanElement errorMessage;

	public MemberDeleteConfirmationError() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	public void setErrorMessage(String theErrorMessage) { errorMessage.setInnerText(theErrorMessage); }
}
