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

public class MessageThreadPollResponseError extends Composite {

	private static MessageThreadPollResponseErrorUiBinder uiBinder = GWT.create(MessageThreadPollResponseErrorUiBinder.class);

	interface MessageThreadPollResponseErrorUiBinder extends UiBinder<Widget, MessageThreadPollResponseError> {}

	@UiField
	SpanElement errorMessage;

	public MessageThreadPollResponseError() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	public void setErrorMessage(String theErrorMessage) { errorMessage.setInnerText(theErrorMessage); }
}
