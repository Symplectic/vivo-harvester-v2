#!/usr/bin/perl
### START JSON FUNCTIONS ###
sub jsonEncodeData {
    my $value = shift;
    $value =~ s/\\/\\\\/g ;
    $value =~ s/\n/\\n/g ;
    $value =~ s/\r/\\r/g ;
    $value =~ s/\t/\\t/g ;
    $value =~ s/"/\\\"/g ;

    $log_line =~ s/(\W\w*[pP]assword:)(.*?),\s/$1*******, /g ;
    return $value;
}

sub jProp{
    my $propName = shift;
    my $propValue = shift;
    my $lastProp = shift;

    my $separator = !$lastProp ? ", " : "";
    return "\"$propName\" : \"" . jsonEncodeData($propValue) . "\"" . $separator;
}
### END JSON FUNCTIONS ###

1;
