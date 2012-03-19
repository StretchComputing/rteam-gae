'use strict';


var rteam = (function(r, $) {

  r.GameView = Backbone.View.extend({
    tagName: 'li',

    initialize: function() {
      _.bindAll(this, 'render');
      this.template = _.template($('#gameListTemplate').html());
    },

    render: function() {
      this.$el.html(this.template(this.model.toJSON()));
      return this;
    }
  });

  r.GamesView = Backbone.View.extend({
    tagName: 'ul',

    initialize: function() {
      _.bindAll(this, 'addGame');
      this.collection.bind('reset', this.render, this);
      this.template = _.template($('#gameEmptyTemplate').html());
    },

    render: function() {
      $(this.el).empty();
      if (this.collection.length <= 0) {
        this.$el.html(this.template());
      } else {
        //this.collection.each(this.addGame);
        var recentGame = this.getMostRecentRelevantGame(this.collection);
        if(recentGame) {
        	this.displayGameInfo(recentGame, this.collection.teamName);
        	this.getActivityInfo(recentGame);
        }
      }
      //this.$el.selectmenu('refresh');
      return this;
    },

    addGame: function(game) {
      r.dump(game);
      this.$el.append(new r.GameView({ model: game }).render().el);
    },
    
    getActivityInfo: function(game) {
        window.activitiesView = new r.ActivitiesView({
            collection: new r.Activities(game.get('gameId'))
         });
        window.activitiesView.collection.fetch();
    },
    
    displayGameInfo: function(game, teamName) {
  	  console.log("displaying score. Most recent game startDate = " + game.get('startDate'));
	  $('#team-name-text').html(teamName);
	  $('#team-opponent-vs').html("vs");
	  var opponentName = game.get('opponent');
  	  $('#opponent-name-text').html(opponentName);
  	  $('#game-date-value').html(rteam.formatDateFormal(game.get('startDate')));
  	  $('#us-score-value').html(game.get('scoreUs'));
  	  $('#them-score-value').html(game.get('scoreThem'));
  	  var interval = game.get('interval');
  	  if(interval == -1) {
  		  interval = "Final";
  	  } else if(interval == 0) {
  		  interval = "pre-game";
  	  }
  	  $('#interval-value').html(interval);
  	  $('#interval-value').css("font-size", "18px");
    },
    
    // Most relevant game: the most recent game for which scoring has started.
    //                     if no games have scoring, return the game that is closes to today's date or before
	getMostRecentRelevantGame: function(collection) {
    	console.log("getMostRecentRelevantGame, collection size = " + collection.size());
    	if(collection.size() == 0) return;
    	
    	// collection ensure games are sorted from most recent to oldest
    	var mostRelevantGame = null;
    	for(var i=0; i<collection.size(); i++) {
    		var game = collection.at(i);
    		
    		console.log("game date = " + game.get('startDate') + " game interval = " + game.get('interval') + " game scoreUs = " + game.get('scoreUs') + " game scoreThem = " + game.get('scoreThem'));
    		if(game.get('interval') != "0" || game.get('scoreUs') != "0" || game.get('scoreThem') != "0") {
    			mostRelevantGame = game;
        		console.log("mostRelevantGame: game date = " + game.get('startDate') + " game interval = " + game.get('interval') + " game scoreUs = " + game.get('scoreUs') + " game scoreThem = " + game.get('scoreThem'));
    			break;
    		}
    	}
    	return mostRelevantGame;
	}
  });

  return r;
})(rteam || {}, jQuery);
