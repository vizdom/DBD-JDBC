use 5.006;
use strict;
use warnings;
use DBI 1.03;
use DBI::DBD;
use ExtUtils::MakeMaker;

WriteMakefile(
    NAME             => 'DBD::JDBC',
    AUTHOR           => q{Gennis Emerson <gemerson@vizdom.com>},
    VERSION_FROM     => 'lib/DBD/JDBC.pm',
    ABSTRACT         => 'DBI proxy driver using JDBC',
    LICENSE          => 'artistic_2',
    PL_FILES         => {},
    MIN_PERL_VERSION => 5.006,
    CONFIGURE_REQUIRES => {
        'ExtUtils::MakeMaker' => 0,
    },
    BUILD_REQUIRES => {
        'Test::More' => 0,
    },
    PREREQ_PM => {
        'DBI'     => '1.48',
        'Convert::BER' => 1.31,
    },
    dist  => { COMPRESS => 'gzip -9f', SUFFIX => 'gz', },
    clean => { FILES => 'test' },
);


package MY;
sub postamble { return main::dbd_postamble(@_); }
sub libscan {
    my ($self, $path) = @_;
    ($path =~ m/\~$/) ? undef : $path;
}
sub test {
    my @cp = grep {-f} <target/lib/*>;
    push(@cp, "\$(CLASSPATH)");

    my $inherited = shift->SUPER::test(@_);
    if ($^O eq 'MSWin32') {
        my $cp = join ';', @cp; 
        $inherited =~ s/\$\(FULLPERLRUN\)/set CLASSPATH=$cp\n\t\$\(FULLPERLRUN\)/g;
    }
    else {
        my $cp = join ':', @cp; 
        $inherited =~ s/\$\(FULLPERLRUN\)/CLASSPATH=$cp \$\(FULLPERLRUN\)/g;
    }
    return $inherited;
}
