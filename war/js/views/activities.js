'use strict';


var rteam = (function(r, $) {

  r.ActivityView = Backbone.View.extend({
    tagName: 'li',

    initialize: function() {
      _.bindAll(this, 'render');
    },

    render: function() {
      return this;
    }
  });

  r.ActivitiesView = Backbone.View.extend({
    tagName: 'ul',

    initialize: function() {
      this.collection.bind('reset', this.render, this);
    },

    render: function() {
      console.log("ActivitiesView::render() entered");
      this.displayActivityInfo(this.collection);
  	  this.getActivitiesWithPhotos(this.collection);
  	  this.displayPhotos();
      return this;
    },
    
    displayActivityInfo: function(collection) {
    	console.log("displayActivityInfo() collection size = " + collection.size());
    	rteam.activityTable.fnClearTable();
    	var template = _.template($('#activityTemplate').html());
    	for(var i=0; i<collection.size(); i++) {
    		var activity = collection.at(i);
    		console.log("***** text = " + activity.get('text') + " poster = " + activity.get('poster') + " created date = " + activity.get('createdDate'));
    		var postHtml = template(activity.toJSON());
    		rteam.activityTable.fnAddData([postHtml]);
    	}
    },
    
    getActivitiesWithPhotos: function(collection) {
    	for(var i=0; i<collection.size(); i++) {
    		var activity = collection.at(i);
    		
    		// keep track of the activities that have a photo associated with them
    		if(activity.get('thumbNail')) {
    			collection.activitiesWithPhotos[collection.activitiesWithPhotos.length] = activity;
    		}
    	}
    	console.log("***** number of activities with photos = " + collection.activitiesWithPhotos.length);
	}, 
	
	displayPhotos: function() {
  	  if(this.collection.activitiesWithPhotos.length > 0) {
  		  // start by showing the first photo
  		var photoUrl = "http://rteamtest.appspot.com/photo/" + this.collection.activitiesWithPhotos[0].get('activityId');
  		$('#photoImg').attr('src', photoUrl);
  		
  		// only show Next button if there are multiple photos to show
  		if(this.collection.activitiesWithPhotos.length > 1) {
  			$('#nextPhoto').css('display', 'block');
  		}
  	  } else {
  		  // TODO get some better default photos
  	  	  $('#photoImg').attr('src', 'http://rteamtest.appspot.com/photo/aglydGVhbXRlc3RyEQsSCEFjdGl2aXR5GMWVxAEM');
  	  }
	}
  });

  return r;
})(rteam || {}, jQuery);
