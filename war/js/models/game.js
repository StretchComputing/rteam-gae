'use strict';

var rteam = (function(r, $) {

  var teamId = r.getParameterByNameFromPage("teamId");
  r.log.debug("teamId from URL = " + teamId);
  
  r.Game = r.BaseModel.extend({
    apiUrl: null, // NA -- only fetch collection
    fields: {
      gameId: null,
      description: null,
      startDate: null,
      opponent: null,
      location: null,
      scoreUs: null,
      scoreThem: null,
      interval: null,
      mvpDisplayName: null
    },

    initialize: function() {
    },
  });


  r.Games = r.BaseCollection.extend({
    model: r.Game,
    apiUrl: '/team/<teamId>/games/', // just a template, real Url set in initialize()
    teamName: 'good guys',

    initialize: function() {
      this.apiUrl = "/team/" + r.getUrlEndSegment() + "/games"
      this.setUrl();
    },

    parse: function(response) {
      this.teamName = response.teamName;
      return response.games;
    },
    
    // sort games from most recent start date to oldest start date
    comparator: function(game1, game2) {
    	// can assume startDate is always non-null
    	var game1StartDate = r.convertStringToDate(game1.get('startDate'));
    	var game2StartDate = r.convertStringToDate(game2.get('startDate'));
    	if(game1StartDate > game2StartDate) {
    		//console.log("game1StartDate = " + game1StartDate.toString() + " is greater than game2StartDate = " + game2StartDate.toString());
    		return -1;
    	} else if(game2StartDate > game1StartDate) {
    		return 1;
    	} else {
    		return 0;
    	}
    }
  });


  return r;
})(rteam || {}, jQuery);
