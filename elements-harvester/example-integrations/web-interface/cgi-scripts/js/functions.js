#!/usr/bin/perl
use CGI;

$SCRIPT=<<END;

function bindSelectAll(targetElement){
    \$(document).keydown(function(event){
        if((event.ctrlKey || event.metaKey) && event.which == 65){
            event.preventDefault();
            var range = document.createRange() // create new range object
            range.selectNodeContents(targetElement.get(0)) // set range to encompass desired element text
            var selection = window.getSelection() // get Selection object from currently user selected text
            selection.removeAllRanges() // unselect any user selected text (if any)
            selection.addRange(range)
            return false;
        }
    });
}

function updateFromJSON(settings){
    //var activeSpan = \$('#recent-harvests h1 span.program-status');
    var lines = settings.targetElement.children('span.log-line');
    var latestLogPosition = settings.targetElement.data("position");
    var firstSeparator = settings.url.indexOf('?') !== -1 ? "&" : "?";
    var jsonUrl = settings.url + firstSeparator +"from=" + encodeURIComponent(latestLogPosition) + (settings.initialLoad ? "&max=" + encodeURIComponent(settings.max) : "");
    var highlightClass = settings.highlightClass != undefined ? settings.highlightClass : "new-line";
    var scrollEnabled = settings.shouldScroll != undefined ? settings.shouldScroll : true;

    var h1 = settings.targetElement.scrollTop() + settings.targetElement.height(),
        h2 = settings.targetElement[0].scrollHeight,
        shouldScroll = h1 == h2 && scrollEnabled;

    \$.getJSON(jsonUrl, function(data){
        var displayReversed = settings.displayDirection === "reverse";
        var dataReversed = settings.dataDirection === "reverse";
        
        var dataToProcess = dataReversed ? data.lines.reverse() : data.lines;

        if(typeof settings.preLineUpdate === "function") settings.preLineUpdate(data, settings);

        //prepare for the addition of new data if any is arriving by removing old highlights.
        if(data.lines.length != 0){
            lines.removeClass(highlightClass);
        }

        \$.each(dataToProcess, function(key, value){
            var highlightClassValue = settings.highlightNew == true ? highlightClass : "";
            var levelClass = "log-level-" + value.level;
            var lineContent = "default value";
            if(typeof settings.lineContent === "function"){
                lineContent = settings.lineContent(value, settings);
            }
            var line = \$('<span class="log-line ' + levelClass + ' ' + highlightClassValue +  '">'+ lineContent + '</span>');
            settings.targetElement.data("position", value.position);
            if(displayReversed) {
                settings.targetElement.prepend(line);
            } else {
                settings.targetElement.append(line);
            }

            var currentLines = settings.targetElement.children('span.log-line');
            if(settings.max != undefined && currentLines.length > settings.max){
                if(reversed){
                    currentLines.last().remove();
                } else {
                    currentLines.first().remove();
                }
            }
        });
        if(shouldScroll){
            settings.targetElement.scrollTop(settings.targetElement[0].scrollHeight);
        }

        if(typeof settings.postLineUpdate === "function") settings.postLineUpdate(data, settings);
    });
}
END

$q = CGI->new;
print $q->header("text/javascript;charset=UTF-8"),
      $SCRIPT;
