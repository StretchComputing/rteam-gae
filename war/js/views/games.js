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
        this.collection.each(this.addGame);
        var recentGame = this.getMostRecentGame(this.collection);
        if(recentGame) this.displayScore(recentGame);
      }
      //this.$el.selectmenu('refresh');
      return this;
    },

    addGame: function(game) {
      r.dump(game);
      this.$el.append(new r.GameView({ model: game }).render().el);
    },
    
    displayScore: function(game) {
  	  console.log("displaying score. Most recent game startDate = " + game.get('startDate'));
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
    
    getMostRecentGame: function(collection) {
  	  console.log("getMostRecentGame, collection size = " + collection.size());
  	  var mostRecentGame = null;
  	  for(var i=0; i<collection.size(); i++) {
  		  var game = collection.at(i);
  		  console.log("game date = " + game.get('startDate'));
  		  if(!mostRecentGame) {
  			  mostRecentGame = game;
  		  } else if(game.startDate != null && mostRecentGame.startDate != null && game.startDate > mostRecentGame.startDate) {
  			mostRecentGame = game; 
  		  }
  	  }
  	  return mostRecentGame;
    }
  });

  return r;
})(rteam || {}, jQuery);
