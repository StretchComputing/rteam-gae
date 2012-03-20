'use strict';


if (!window.console) {
  window.console = {
    log: function () {}
  };
}

// The main namespace for our application
var rteam = (function(r, $) {


  r.log = {
    logLevels: {
      error: 1,
      info: 5,
      debug: 10
    },

    logLevel: function() {
      return this.logLevels.debug;
    },

    error: function(message) {
      if (this.logLevel() < this.logLevels.error) { return; }
      this.base('Error: ' + message);
    },

    info: function(message) {
      if (this.logLevel() < this.logLevels.info) { return; }
      this.base('Info: ' + message);
    },

    debug: function(message) {
      if (this.logLevel() < this.logLevels.debug) { return; }
      this.base('Debug: ' + message);
    },

    base: function(message) {
      console.log(message);
    }
  };


  r.setCookie = function(token) {
    Cookie.set('token', token, 9000, '\/');
  },


  r.unsetCookie = function(token) {
    Cookie.unset('token', '/');
  },


  r.dump = function(object) {
    console.log(JSON.stringify(object));
  };


  // Change to a new HTML page.
  r.changePage = function(page, area) {
    var
      base,
      newPage,
      pages = {
        root: '\/',
        applications: '',
        settings: '#settings'
      };

    switch (area) {
      case 'signup':
        base = '';
        break;
      case 'admin':
        base = '\/html5\/admin';
        break;
      default:
        base = '\/html5';
        break;
    }


    if (pages[page] === undefined) {
      r.log.error("rteam.changePage: page '" + page + "' not found.");
      return;
    }

    newPage = base + pages[page];
    r.log.debug("rteam.changePage: page '" + newPage + "'.");
    window.location = newPage;
  };


  r.getContentDiv = function() {
    return $.mobile.activePage.find(":jqmData(role='content')");
  };


  // message: The error message to display.
  // el: If not specified, we'll use the active page's content area.
  r.flashError = function(message, el) {
    var flash, selector;

    el = el || r.getContentDiv();

    selector = '.flash.error';
    el.find(selector).remove();

    message = message || 'An unknown error occurred. Please reload the page to try again.';
    flash = $('<div>', {
      // TODO have Terry refactor this
      // removed due to IE error
      //class: 'flash error',
      text: message
    });
    $(el).prepend(flash);
  };


  // Add a property to an object, but only if it is defined and not blank.
  r.addProperty = function(object, property, value) {
    if (object && property && value) {
      object[property] = value;
    }
  };


  // Returns the value of a named parameter from a given JQM URL.
  r.getParameterByName = function (url, name) {
    var match = new RegExp('[?&]' + name + '=([^&]*)').exec(url);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
  };

  r.getParameterByNameFromPage = function (name)
  {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    if(results == null)
      return "";
    else
      return decodeURIComponent(results[1].replace(/\+/g, " "));
  }
  
  // for URL: http://site.com/abc  abc is returned
  r.getUrlEndSegment = function ()
  {
	 var segments = window.location.href.split("/");
	 return segments[segments.length - 1];
  }
  
  // Build a query string from an object.
  //
  // props: the object containing the name/value pairs for the query string
  r.buildQueryString = function (props) {
    var prop, query;

    query = "?";
    for (prop in props) {
      query += prop + '=' + props[prop] + '&';
    }
    query = query.slice(0, query.length - 1);

    return query;
  };


  // Pull the apiStatus value out of an HTTP error response.
  r.getApiStatus = function(responseText) {
    return JSON.parse(responseText).apiStatus;
  };


  // Simple RegEx to ensure a valid phone number format.
  r.isValidPhoneNumber = function (phoneNumber) {
    return (/^\(?([0-9]{3})\)?[\-. ]?([0-9]{3})[\-. ]?([0-9]{4})$/).test(phoneNumber);
  };


  // Simple RegEx to ensure a valid email address format.
  r.isValidEmailAddress = function (emailAddress) {
    return (/^[a-z0-9!#$%&'*+\/=?\^_`{|}~\-]+(?:\.[a-z0-9!#$%&'*+\/=?\^_`{|}~\-]+)*@(?:[a-z0-9](?:[a-z0-9\-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9\-]*[a-z0-9])?$/).test(emailAddress);
  };

  
  // only supports format: YYYY-MM-DD hh:mm
  r.convertStringToDate = function(theDateStr) {
	  //console.log("convertStringToDate(): theDateStr = " + theDateStr);
	  if(!theDateStr) return null;
	  
	  var reggie = /(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})/;
	  var dateArray = reggie.exec(theDateStr);
	  //console.log("dataArray = " + dateArray.toString());
	  var date = new Date(
	      (+dateArray[1]),
	      (+dateArray[2])-1, // Careful, month starts at 0!
	      (+dateArray[3]),
	      (+dateArray[4]),
	      (+dateArray[5])
	  );	  
	  //console.log("***** date from string to date to string = " + date.toString());
	  return date;
  };
  
  // only supports format: YYYY-MM-DD hh:mm
  r.formatDate = function(theDateStr) {
	  var hourFormat = ["12 am", "1 am", "2 am", "3 am", "4 am", "5 am", "6 am", "7 am", "8 am", "9 am", "10 am", "11 am", 
	                    "12 pm", "1 pm", "2 pm", "3 pm", "4 pm", "5 pm", "6 pm", "7 pm", "8 pm", "9 pm", "10 pm", "11 pm"];
	  var dateObj = r.convertStringToDate(theDateStr);
	  var date = dateObj.getDate();
	  var month = dateObj.getMonth() + 1;
	  var year = dateObj.getYear();
	  var hours = dateObj.getHours();
	  return month.toString() + "/" + date + " " + hourFormat[hours];
  }
  
  // only supports format: YYYY-MM-DD hh:mm
  r.formatDateFormal = function(theDateStr) {
	  var hourFormat = ["12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", 
	                    "12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"];
	  var monthFormat = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
	  var dayFormat = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
	  var dateObj = r.convertStringToDate(theDateStr);
	  var day = dateObj.getDay();
	  var date = dateObj.getDate();
	  var month = dateObj.getMonth();
	  var year = dateObj.getYear();
	  var hours = dateObj.getHours();
	  var ampm = hours < 11 ? "AM" : "PM";
	  var output = "<span id='game-date-day'>" + dayFormat[day] + ", " +  "</span>" +
	  			   "<span id='game-date-month'>" + monthFormat[month] + "</span>" +
	  			   "<span id='game-date-date'>" + date + "</span>" +
	  			   "<span id='game-date-hour'>" + hourFormat[hours] + "</span>" +
	  			   "<span id='game-date-ampm'>" + ampm + "</span>";
	  return output;
  }

  return r;
}(rteam || {}, jQuery));


// This is here so we automatically get page loading messages when Ajax requests start and
// they are hidden when the Ajax requests are complete.
(function() {
  var hidePageLoadingMessage, pageLoad, pageLoadCount, showPageLoadingMessage;

  pageLoadCount = 0;
  pageLoad = function (operator) {
    switch (operator) {
    case 'decrement':
      pageLoadCount -= pageLoadCount === 0 ? 0 : 1;
      break;
    case 'increment':
      pageLoadCount += 1;
      break;
    default:
      window.console.log('pageLoadingCount called with inappropriate operator.');
    }
    return pageLoadCount;
  };


  // Manage showing/hiding the page loading message based on the number of times it's been called.
  hidePageLoadingMessage = function () {
    if (pageLoad('decrement') <= 0) {
      $.mobile.hidePageLoadingMsg();
    }
  };

  showPageLoadingMessage = function () {
    pageLoad('increment');
    $.mobile.showPageLoadingMsg();
  };

  $('html').ajaxSend(function(event, jqXHR, settings) {
    rteam.log.debug('ajaxSend: ' + settings.url);
    showPageLoadingMessage();
  });
  $('html').ajaxComplete(function(event, jqXHR, settings) {
    rteam.log.debug('ajaxComplete: ' + settings.url);
    hidePageLoadingMessage();
  });
  
})();
