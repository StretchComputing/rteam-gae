package com.stretchcom.rteam.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

public class MessageThreadPollResponse extends Composite {

	private static MessageThreadPollResponseUiBinder uiBinder = GWT.create(MessageThreadPollResponseUiBinder.class);

	interface MessageThreadPollResponseUiBinder extends UiBinder<Widget, MessageThreadPollResponse> {}

	@UiField
    Grid membersGrid;
	
	@UiField
	SpanElement numOfMembers;

	public MessageThreadPollResponse() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	public void setNumOfMembers(String theNumOfMembers) { numOfMembers.setInnerText(theNumOfMembers); }
	
	public void setMembersTable(List<Member> theMembers) {
		int numOfRows = theMembers.size() + 1; // first row is column names
		membersGrid.clear(true);
		membersGrid.resize(numOfRows, 3);
		
		// set the column headers first
		membersGrid.setHTML(0, 0, "Member Name");
		membersGrid.setHTML(0, 1, "Reply");
		membersGrid.setHTML(0, 2, "Reply Date");
		membersGrid.getRowFormatter().setStyleName(0, "gridColumnHeader");
		
        for (int rowNumber=1, memIndex=0; rowNumber < numOfRows; ++rowNumber, ++memIndex) {
        	String memberNameHtml = theMembers.get(memIndex).getMemberFullName() == null ? "" : theMembers.get(memIndex).getMemberFullName();
        	membersGrid.setHTML(rowNumber, 0, memberNameHtml);
        	
        	String memberReplyHtml = theMembers.get(memIndex).getReply() == null ? "" : theMembers.get(memIndex).getReply();
        	membersGrid.setHTML(rowNumber, 1, memberReplyHtml);
        	
        	String memberReplyDateHtml = theMembers.get(memIndex).getReplyDate() == null ? "" : theMembers.get(memIndex).getReplyDate();
        	membersGrid.setHTML(rowNumber, 2, memberReplyDateHtml);
        }
        
        for (int i=1; i < membersGrid.getRowCount(); i++) {
            for (int j=0; j < membersGrid.getCellCount(i); j++) {
                if ((j % 2) == 0) {
                	membersGrid.getCellFormatter().setStyleName(i, j, "gridCell");
                } else {
                	membersGrid.getCellFormatter().setStyleName(i, j, "gridCell");
                }
            }
        }        
	}
	
}
