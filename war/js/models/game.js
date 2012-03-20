var rteam = (function (r, $) {
  'use strict';

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
  });


  r.Games = r.BaseCollection.extend({
    model: r.Game,
    apiUrl: '/team/<teamId>/games/', // just a template, real Url set in initialize()
    teamName: 'good guys',

    initialize: function () {
      this.apiUrl = "/team/" + r.getUrlEndSegment() + "/games";
      this.setUrl();
    },

    parse: function (response) {
      this.teamName = response.teamName;
      return response.games;
    },

    // sort games from most recent start date to oldest start date
    comparator: function (game1, game2) {
      var game1StartDate, game2StartDate;

      // can assume startDate is always non-null
      game1StartDate = r.convertStringToDate(game1.get('startDate'));
      game2StartDate = r.convertStringToDate(game2.get('startDate'));
      if (game1StartDate > game2StartDate) {
        //console.log("game1StartDate = " + game1StartDate.toString() + " is greater than game2StartDate = " + game2StartDate.toString());
        return -1;
      }
      if (game2StartDate > game1StartDate) {
        return 1;
      }
      return 0;
    }
  });


  return r;
}(rteam || {}, jQuery));
