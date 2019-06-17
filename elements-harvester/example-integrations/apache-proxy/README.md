# Apache proxy

This directory contains example virtual host files to set Apache up as a reverse proxy in front of the Java servlet container running Vivo (e.g. *Tomcat*).

## Assumptions
For the purposes of these examples we assume that container is running on the same server as apache on port 8080 (*localhost:8080*). We also assume that Vivo has been deployed into the servlet container named as "ROOT" (so that it is deployed at "/"), and that therefore Vivo's companion solr webapp is deployed at "ROOTsolr".

***Note:** All these examples are based on CENTOS 7, but much of the configuration will be applicable to other distributions.*

## The Examples
The examples cover three different use cases for different deployments of Vivo:
  1. Plain http: *vivo.conf*
  2. Secure https: *vivo_ssl.conf*
  3. Secure https with the harvester web-interface deployed: *vivo_ssl_with_web_interface.conf*

### Plain http (*vivo.conf*)
This file is quite simple, it defines an apache virtual host on port 80, and sets up a proxy to localhost:8080 using *ProxyPass* directives. The only feature of particular note is the line:

    ProxyPass /ROOTsolr/ !

This ensures that no external requests reach Vivo's companion solr app.

***Note:** There are other ways to achieve the same results (for example you could use the AJP connector and Valve's in Tomcat's context files) but we have found this to be the easiest and least invasive approach.*

### Secure https (*vivo_ssl.conf*)
This file is much longer, but much of it is cribbed from the default apache ssl.conf. The main changes are:
  1. A virtual host on port 80 configured to redirect all requests to https on port 443.
  2. A secure virtual host on port 443 configured to proxy to localhost:8080 using *ProxyPass* directives
  3. *ProxyPass* directive in secure virtual host to prevent access to /ROOTsolr
  4. Alterations to *SSLCipherSuite* and *SSLHonorCipherOrder* to achieve a good SSL labs score

### Secure https with Web Interface (*vivo_ssl_with_web_interface.conf*)
The harvester ships with a set of apache CGI scripts (and other tools) to enable a simple web interface (see the *web-interface* folder in the *examples/example-integrations* folder).
The file *vivo_ssl_with_web_interface.conf* is almost identical to *vivo_ssl.conf*, with additions that help enable this web interface. The additional features in the secure virtual host are:
  1. ScriptAlias to pipe requests to a selected path (*harvesterControl*) to the cgi-bin
  2. Additional *ProxyPass* directives to prevent the proxy intercepting requests to that path (*harvesterControl*)
  3. A RewriteEngine to forward any requests to */harvesterControl* to */harvesterControl/default*
  4. Additions to <Directory "var/www/cgi-bin"> to require user authentication when accessing the cgi-bin

Most of these features work so that requests to a particular path (*harvesterControl*) are piped to call scripts deployed in the configured cgi-bin instead of being proxied to Tomcat.

***Note:** The final change ensures that access to these scripts is protected, and requires you to set up an authority file using the htpasswd utility. Details of this are beyond the scope of this document.*

## SSL Certificate Configuration
It is important to note that neither of the "https" *.conf* sample files are actually configured to work with SSL out of the box as you need to acquire an SSL certificate, that is valid for your server, and configure apache to use it.

***Note:** Apache SSL configuration is generally beyond the scope of this document.*

At a minimum you will need to edit your conf file to set the *SSLCertificateFile* and *SSLCertificateKeyFile* parameters, pointing to the relevant SSL certificate files (e.g. crt and pem files respectively). It is also strongly recommended that you provide the *SSLCertificateChainFile* (e.g. ca_bundle file) to create a secure SSL chain.


