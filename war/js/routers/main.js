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
  }
);

