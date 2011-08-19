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

public class MemberDeleteConfirmation extends Composite {

	private static MemberDeleteConfirmationUiBinder uiBinder = GWT.create(MemberDeleteConfirmationUiBinder.class);

	interface MemberDeleteConfirmationUiBinder extends UiBinder<Widget, MemberDeleteConfirmation> {}
	
	@UiField
	SpanElement emailAddress;

	@UiField
	SpanElement fullName;

	public MemberDeleteConfirmation() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	public void setFullName(String theFullName) { fullName.setInnerText(theFullName); }
	
	public void setEmailAddress(String theEmailAddress) { emailAddress.setInnerText(theEmailAddress); }

}
