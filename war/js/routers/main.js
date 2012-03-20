var rteam = (function (r, $) {
  'use strict';

  r.scoreboard = Backbone.Router.extend({
    routes: {
      '/:teamId': 'defaultRoute'
    },

    initialize: function () {
      this.gamesView = new r.GamesView({
        el: $('#container'),
        collection: new r.Games()
      });
      this.gamesView.collection.fetch();
    },

    defaultRoute: function (teamId) {
    }
  });

  return r;
}(rteam || {}, jQuery));

// call jQuery onLoad registration function to create the router which gets everything going
$(function () {
  'use strict';

  var r_scoreboard = new rteam.scoreboard();

  Backbone.history.start();
  console.log("Backbone history started ...");

  rteam.init();
  rteam.activityTable.fnDraw();

  $("#nextPhoto").click(function () {
    var
      currentPhoto,
      numOfPhotos = window.activitiesView.collection.activitiesWithPhotos.length,
      photoUrl;

    if (numOfPhotos > 2) {
      currentPhoto = window.activitiesView.collection.currentPhoto;
      if ((currentPhoto + 1) < numOfPhotos) {
        currentPhoto += 1;
        window.activitiesView.collection.currentPhoto = currentPhoto;
        photoUrl = "http://rteamtest.appspot.com/photo/" + window.activitiesView.collection.activitiesWithPhotos[currentPhoto].get('activityId');
        $('#photoImg').attr('src', photoUrl);
      }
    }
  });
});

