'use strict';


var rteam = (function(r, $) {

  r.ActivityView = Backbone.View.extend({
    tagName: 'li',

    initialize: function() {
      _.bindAll(this, 'render');
    },

    render: function() {
      return this;
    }
  });

  r.ActivitiesView = Backbone.View.extend({
    tagName: 'ul',

    initialize: function() {
      this.collection.bind('reset', this.render, this);
    },

    render: function() {
      console.log("ActivitiesView::render() entered");
      this.displayActivityInfo(this.collection);
      return this;
    },
    
    displayActivityInfo: function(collection) {
    	console.log("displayActivityInfo() collection size = " + collection.size());
    	rteam.activityTable.fnClearTable();
    	var template = _.template($('#activityTemplate').html());
    	for(var i=0; i<collection.size(); i++) {
    		var activity = collection.at(i);
    		console.log("***** text = " + activity.get('text') + " poster = " + activity.get('poster') + " created date = " + activity.get('createdDate'));
    		var postHtml = template(activity.toJSON());
    		rteam.activityTable.fnAddData([postHtml]);
    		rteam.activityTable.fnAddData([postHtml]);
    		rteam.activityTable.fnAddData([postHtml]);
    		rteam.activityTable.fnAddData([postHtml]);
    	}
    }
  });

  return r;
})(rteam || {}, jQuery);
