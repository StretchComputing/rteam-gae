'use strict';


var rteam = (function(r, $) {


  r.GameView = Backbone.View.extend({
    tagName: 'li',

    initialize: function() {
      _.bindAll(this, 'render');
      this.template = _.template($('#gameListTemplate').html());
    },

    render: function() {
      this.$el.html(this.template(this.model));
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
      }
      //this.$el.selectmenu('refresh');
      return this;
    },

    addGame: function(app) {
      this.$el.append(new r.GameView({ model: app }).render().el);
    }
  });


  return r;
})(rteam || {}, jQuery);
