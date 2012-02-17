'use strict';

var rteam = (function(r, $) {

  var teamId = r.getParameterByNameFromPage("teamId");
  r.log.debug("teamId from URL = " + teamId);
  
  r.Game = r.BaseModel.extend({
    apiUrl: '/team/<teamId>/games', // just a template to follow
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
      this.apiUrl = "/team/" + r.getUrlEndSegment() + "/games"
      this.setUrl();
    },
  });


  r.Games = r.BaseCollection.extend({
    model: r.Game,
    apiUrl: '/team/<teamId>/games/', // just a template, real Url set in initialize()

    initialize: function() {
      this.apiUrl = "/team/" + r.getUrlEndSegment() + "/games"
      this.setUrl();
    },

    parse: function(response) {
      return response.games;
    }
  });


  return r;
})(rteam || {}, jQuery);