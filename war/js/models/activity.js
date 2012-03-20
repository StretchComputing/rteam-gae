var rteam = (function (r, $) {
  'use strict';

  var teamId = r.getParameterByNameFromPage("teamId");
  r.log.debug("teamId from URL = " + teamId);

  r.Activity = r.BaseModel.extend({
    apiUrl: null, // NA -- only fetch collection
    fields: {
      activityId: null,
      text: null,
      createdDate: null,
      formatedCreatedDate: null,
      cacheId: null,
      numberOfLikeVotes: null,
      numberOfDislikeVotes: null,
      thumbNail: null,
      isVideo: null,
      useTwitter: null,
      poster: null
    },

    initialize: function () {
    }
  });


  r.Activities = r.BaseCollection.extend({
    model: r.Activity,
    apiUrl: '/team/<teamId>/activities/<timeZone>', // just a template, real URL set in initialize()
    activitiesWithPhotos: [],
    currentPhoto: 0,

    initialize: function (gameId) {
      this.apiUrl = "/team/" + r.getUrlEndSegment() + "/activities?eventId=" + gameId + "&eventType=game";
      console.log("Activities::initialize()  apiUrl = " + this.apiUrl);
      this.setUrl();
    },

    parse: function (response) {
      return response.activities;
    },

    // sort activities from most recent created date to oldest start date
    comparator: function (activity1, activity2) {
      var game1CreatedDate, game2CreatedDate;
      console.log("Activities::comparator entered");

      // can assume createdDate is always non-null
      game1CreatedDate = r.convertStringToDate(activity1.get('createdDate'));
      game2CreatedDate = r.convertStringToDate(activity2.get('createdDate'));
      if (game1CreatedDate > game2CreatedDate) {
        console.log("game1CreatedDate = " + game1CreatedDate.toString() + " is greater than game2CreatedDate = " + game2CreatedDate.toString());
        return -1;
      }
      if (game2CreatedDate > game1CreatedDate) {
        return 1;
      }
      return 0;
    }
  });


  return r;
}(rteam || {}, jQuery));
