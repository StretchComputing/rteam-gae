'use strict';

var rteam = (function(r, $) {
     r.scoreboard = Backbone.Router.extend({
            routes: {
                '/:teamId': 'defaultRoute'
            },

            initialize: function() {
              this.gamesView = new r.GamesView({
                el: $('#container'),
                collection: new r.Games()
             });
              this.gamesView.collection.fetch();
            },

            defaultRoute: function(teamId) {
            }

    });

  return r;
})(rteam || {}, jQuery);

// call jQuery onLoad registration function to create the router which gets everything going
$(function() {
    var r_scoreboard = new rteam.scoreboard;
    Backbone.history.start();
    console.log("Backbone history started ...");
    rteam.init(); 
    rteam.activityTable.fnDraw();
    
    $("#nextPhoto").click(function() {
    	var numOfPhotos = window.activitiesView.collection.activitiesWithPhotos.length;
    	if(numOfPhotos > 2) {
    		var currentPhoto = window.activitiesView.collection.currentPhoto;
    		if((currentPhoto + 1) < numOfPhotos) {
    			currentPhoto += 1;
    			window.activitiesView.collection.currentPhoto = currentPhoto;
          		var photoUrl = "http://rteamtest.appspot.com/photo/" + this.collection.activitiesWithPhotos[currentPhoto].get('activityId');
          		$('#photoImg').attr('src', photoUrl);
    		}
    	}
    });    
  }
);

