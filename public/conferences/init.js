// http://www.daterangepicker.com/
(function(){
    var dateFormat = 'DD/MM/YYYY';

    // see common.models.utils.DateRange & common.views.forms.inputDateRange
    $('input.daterange').daterangepicker({
        autoUpdateInput: false,
        autoApply: true,
        locale: {
            format: dateFormat
        }
    });
    $('input.daterange').on('apply.daterangepicker', function(ev, picker) {
        $(this).val(picker.startDate.format(dateFormat) + ' - ' + picker.endDate.format(dateFormat));
    });
    $('input.daterange').on('cancel.daterangepicker', function(ev, picker) {
        $(this).val('');
    });

    // see common.views.forms.inputDate
    $('input.datepicker').daterangepicker({
        singleDatePicker: true,
        autoUpdateInput: false,
        locale: {
            format: dateFormat
        }
    });
    $('input.datepicker').on('apply.daterangepicker', function(ev, picker) {
        $(this).val(picker.startDate.format(dateFormat));
    });
    $('input.datepicker').on('cancel.daterangepicker', function(ev, picker) {
        $(this).val('');
    });
})();

// https://select2.github.io/
(function(){
    if($('.select2-tags')[0]){
        $('.select2-tags').each(function(){
            $(this).select2({
                width: '100%',
                theme: 'bootstrap',
                placeholder: $(this).attr('placeholder'),
                tags: true,
                tokenSeparators: [',']
            });
        });
    }
})();

// automatically fill conference form with website metas
(function(){
    // TODO : on input#social_twitter_account fetch twitter img profile to fill input#logo ...
    $('input#siteUrl').on('change', function(){
        var url = $(this).val();
        if(isUrl(url)){
            $.get('/tools/scrapers/utils/metas?url='+url, function(metas){
                setValue(metas, 'title.0', 'input#name');
                setValue(metas, 'description.0', 'textarea#description');
                setValue(metas, 'all.twitter:site.0', 'input#social_twitter_account', function(str){ return str ? str.replace('@', '') : ''; });
                setTags(metas, 'keywords', 'select#tags');
            });
        }
    });
    function isUrl(str) {
        var regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
        return regexp.test(str);
    }
    function setValue(data, dataSelector, eltSelector, dataTransform) {
        var elt = $(eltSelector);
        if(elt.val() === ""){
            var value = getSafe(data, dataSelector);
            elt.val(dataTransform ? dataTransform(value) : value);
        }
    }
    function setTags(data, dataSelector, eltSelector) {
        var elt = $(eltSelector);
        if(elt.val() === null){
            var tags = getSafe(data, dataSelector) || [];
            tags.map(function(tag){
                if(elt.find('option[value="'+tag+'"]').length === 0){
                    elt.append('<option value="'+tag+'">'+tag+'</option>');
                }
            });
            elt.select2('val', tags);
        }
    }
    function getSafe(obj, path) {
        if(typeof path === 'string')  { return getSafe(obj, path.split('.')); }
        if(!Array.isArray(path))      { return obj; }
        if(path.length === 0 || !obj) { return obj; }
        var newObj = obj[path[0]];
        var newPath = path.slice(1);
        return getSafe(newObj, newPath);
    }
})();

// save and fill conference user data
(function(){
    var storageKey = 'conference-createdBy';
    setFormUser(getStorageUser());
    $('form.conference-form').on('submit', function(){
        setStorageUser(getFormUser());
    });
    function getFormUser(){
        return {
            name: $('input#createdBy_name').val(),
            email: $('input#createdBy_email').val(),
            siteUrl: $('input#createdBy_siteUrl').val(),
            twitter: $('input#createdBy_twitter').val(),
            public: $('input#createdBy_public').prop('checked')
        };
    }
    function setFormUser(user){
        function setInput(elt, value){ if(elt.val() === ""){ elt.val(value); } }
        if(user){
            setInput($('input#createdBy_name'), user.name);
            setInput($('input#createdBy_email'), user.email);
            setInput($('input#createdBy_siteUrl'), user.siteUrl);
            setInput($('input#createdBy_twitter'), user.twitter);
            $('input#createdBy_public').prop('checked', user.public);
        }
    }
    function getStorageUser(){
        if(localStorage){
            var json = localStorage.getItem(storageKey);
            try {
                return JSON.parse(json || '{}');
            } catch(e) {
                console.warn('Unable to parse to JSON', json);
            }
        }
    }
    function setStorageUser(user){
        if(localStorage && user){
            localStorage.setItem(storageKey, JSON.stringify(user));
        }
    }
})();

// http://fullcalendar.io/
(function(){
    var fullCalendarDateFormat = 'YYYY-MM-DD';
    var apiDateFormat = 'DD/MM/YYYY';
    var apiResults = {};
    $(document).ready(function() {
        $('.full-calendar').each(function(){
            var $elt = $(this);
            $elt.fullCalendar({
                lang: $elt.attr('lang') || 'en',
                header: {
                    left: '',
                    center: 'title',
                    right: 'today prev,next'
                },
                defaultDate: tryInt($elt.attr('default-date')) || null,
                events: getEvents($elt) || [],
                height: tryInt($elt.attr('height')),
                editable: false,
                eventLimit: true
            });
        });
    });
    function getEvents($elt) {
        // static list of events
        var eventsJson = $elt.attr('events');
        if(eventsJson){ return JSON.parse(eventsJson); }
        // events fetched from API
        var remoteEventsJson = $elt.attr('remote-events');
        if(remoteEventsJson){
            var config = JSON.parse(remoteEventsJson);
            return function(start, end, timezone, callback){
                var url = config.searchUrl.replace('START_DATE', start.format(apiDateFormat)).replace('END_DATE', end.format(apiDateFormat));
                if(apiResults[url]){
                    callback(apiResults[url]);
                } else {
                    $.ajax({
                        url: config.searchUrl.replace('START_DATE', start.format(apiDateFormat)).replace('END_DATE', end.format(apiDateFormat)),
                        success: function(data) {
                            apiResults[url] = data.result.map(function(e){
                                return {
                                    title: e.name,
                                    start: moment(e.start).format(fullCalendarDateFormat),
                                    end: moment(e.end).add(1, 'days').format(fullCalendarDateFormat), // fullCalendar exclude last day
                                    url: config.eventUrl.replace('ID', e.id)
                                };
                            });
                            callback(apiResults[url]);
                        }
                    });
                }
            };
        }
    }
    function tryInt(str) {
        try {
            return parseInt(str);
        } catch (e) {
            return str;
        }
    }
})();

// called by google maps
function googleMapsInit(){
    // GMapPlace picker (https://developers.google.com/maps/documentation/javascript/examples/places-autocomplete?hl=fr)
    (function(){
        $('.gmap-place-picker').each(function() {
            var $elt = $(this);
            var autocomplete = new google.maps.places.Autocomplete($elt.find('input[type="text"]').get(0));
            autocomplete.addListener('place_changed', function() {
                var place = autocomplete.getPlace(); // cf https://developers.google.com/maps/documentation/javascript/3.exp/reference?hl=fr#PlaceResult
                var formattedPlace = formatPlace(place);
                fillForm($elt, formattedPlace);
            });
        });

        function fillForm($elt, formattedPlace){
            $elt.find('input[type="hidden"].place-id').val(formattedPlace.id);
            $elt.find('input[type="hidden"].place-name').val(formattedPlace.name);
            $elt.find('input[type="hidden"].place-streetNo').val(formattedPlace.streetNo);
            $elt.find('input[type="hidden"].place-street').val(formattedPlace.street);
            $elt.find('input[type="hidden"].place-postalCode').val(formattedPlace.postalCode);
            $elt.find('input[type="hidden"].place-locality').val(formattedPlace.locality);
            $elt.find('input[type="hidden"].place-country').val(formattedPlace.country);
            $elt.find('input[type="hidden"].place-formatted').val(formattedPlace.formatted);
            $elt.find('input[type="hidden"].place-lat').val(formattedPlace.geo.lat);
            $elt.find('input[type="hidden"].place-lng').val(formattedPlace.geo.lng);
            $elt.find('input[type="hidden"].place-url').val(formattedPlace.url);
            $elt.find('input[type="hidden"].place-website').val(formattedPlace.website);
            $elt.find('input[type="hidden"].place-phone').val(formattedPlace.phone);
        }
        function formatPlace(place){
            function formatAddressComponents(components){
                function findByType(components, type){
                    var c = components.find(function(c){ return c.types.indexOf(type) >= 0; });
                    return c ? c.long_name : undefined;
                }
                return {
                    street_number: findByType(components, "street_number"), // ex: "119"
                    route: findByType(components, "route"), // ex: "Boulevard Voltaire"
                    postal_code: findByType(components, "postal_code"), // ex: "75011"
                    locality: findByType(components, "locality"), // ex: "Paris"
                    country: findByType(components, "country"), // ex: "France"
                    administrative_area: {
                        level_1: findByType(components, "administrative_area_level_1"), // ex: "Île-de-France"
                        level_2: findByType(components, "administrative_area_level_2"), // ex: "Paris"
                        level_3: findByType(components, "administrative_area_level_3"),
                        level_4: findByType(components, "administrative_area_level_4"),
                        level_5: findByType(components, "administrative_area_level_5")
                    },
                    sublocality: {
                        level_1: findByType(components, "sublocality_level_1"),
                        level_2: findByType(components, "sublocality_level_2"),
                        level_3: findByType(components, "sublocality_level_3"),
                        level_4: findByType(components, "sublocality_level_4"),
                        level_5: findByType(components, "sublocality_level_5")
                    }
                };
            }
            var components = formatAddressComponents(place.address_components);
            return {
                id: place.place_id,
                name: place.name,
                streetNo: components.street_number,
                street: components.route,
                postalCode: components.postal_code,
                locality: components.locality,
                country: components.country,
                formatted: place.formatted_address,
                geo: {
                    lat: place.geometry.location.lat(),
                    lng: place.geometry.location.lng()
                },
                url: place.url,
                website: place.website,
                phone: place.international_phone_number
            };
        }
    })();
}
