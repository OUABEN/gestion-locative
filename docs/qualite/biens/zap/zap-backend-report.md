# ZAP Scanning Report

ZAP by [Checkmarx](https://checkmarx.com/).


## Summary of Alerts

| Risk Level | Number of Alerts |
| --- | --- |
| High | 0 |
| Medium | 1 |
| Low | 2 |
| Informational | 3 |




## Insights

| Level | Reason | Site | Description | Statistic |
| --- | --- | --- | --- | --- |
| Medium | Exceeded High | http://host.docker.internal:8081 | Percentage of responses with status code 4xx | 99 % |
| Info | Informational | http://host.docker.internal:8081 | Percentage of responses with status code 2xx | 5 % |
| Info | Informational | http://host.docker.internal:8081 | Percentage of endpoints with content type application/json | 100 % |
| Info | Informational | http://host.docker.internal:8081 | Percentage of endpoints with method DELETE | 29 % |
| Info | Informational | http://host.docker.internal:8081 | Percentage of endpoints with method GET | 51 % |
| Info | Informational | http://host.docker.internal:8081 | Percentage of endpoints with method PATCH | 6 % |
| Info | Informational | http://host.docker.internal:8081 | Percentage of endpoints with method POST | 6 % |
| Info | Informational | http://host.docker.internal:8081 | Percentage of endpoints with method PUT | 6 % |
| Info | Informational | http://host.docker.internal:8081 | Count of total endpoints | 31    |







## Alerts

| Name | Risk Level | Number of Instances |
| --- | --- | --- |
| Cross-Domain Misconfiguration | Medium | Systemic |
| Server Leaks Information via "X-Powered-By" HTTP Response Header Field(s) | Low | Systemic |
| X-Content-Type-Options Header Missing | Low | 1 |
| A Client Error response code was returned by the server | Informational | 31 |
| Non-Storable Content | Informational | Systemic |
| Storable and Cacheable Content | Informational | 1 |




## Alert Detail



### [ Cross-Domain Misconfiguration ](https://www.zaproxy.org/docs/alerts/10098/)



##### Medium (Medium)

### Description

Web browser data loading may be possible, due to a Cross Origin Resource Sharing (CORS) misconfiguration on the web server.

* URL: http://host.docker.internal:8081/api/biens/id
  * Node Name: `http://host.docker.internal:8081/api/biens/id`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `Access-Control-Allow-Origin: *`
  * Other Info: `The CORS misconfiguration on the web server permits cross-domain read requests from arbitrary third party domains, using unauthenticated APIs on this domain. Web browser implementations do not permit arbitrary third parties to read the response from authenticated APIs, however. This reduces the risk somewhat. This misconfiguration could be used by an attacker to access data that is available in an unauthenticated manner, but which uses some other form of security, such as IP address white-listing.`
* URL: http://host.docker.internal:8081/api/biens%3Fville=ville&statut=LIBRE&type=type
  * Node Name: `http://host.docker.internal:8081/api/biens (statut,type,ville)`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `Access-Control-Allow-Origin: *`
  * Other Info: `The CORS misconfiguration on the web server permits cross-domain read requests from arbitrary third party domains, using unauthenticated APIs on this domain. Web browser implementations do not permit arbitrary third parties to read the response from authenticated APIs, however. This reduces the risk somewhat. This misconfiguration could be used by an attacker to access data that is available in an unauthenticated manner, but which uses some other form of security, such as IP address white-listing.`
* URL: http://host.docker.internal:8081/api/biens/id
  * Node Name: `http://host.docker.internal:8081/api/biens/id`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `Access-Control-Allow-Origin: *`
  * Other Info: `The CORS misconfiguration on the web server permits cross-domain read requests from arbitrary third party domains, using unauthenticated APIs on this domain. Web browser implementations do not permit arbitrary third parties to read the response from authenticated APIs, however. This reduces the risk somewhat. This misconfiguration could be used by an attacker to access data that is available in an unauthenticated manner, but which uses some other form of security, such as IP address white-listing.`
* URL: http://host.docker.internal:8081/api/biens/id/disponibilite
  * Node Name: `http://host.docker.internal:8081/api/biens/id/disponibilite`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `Access-Control-Allow-Origin: *`
  * Other Info: `The CORS misconfiguration on the web server permits cross-domain read requests from arbitrary third party domains, using unauthenticated APIs on this domain. Web browser implementations do not permit arbitrary third parties to read the response from authenticated APIs, however. This reduces the risk somewhat. This misconfiguration could be used by an attacker to access data that is available in an unauthenticated manner, but which uses some other form of security, such as IP address white-listing.`
* URL: http://host.docker.internal:8081/api/biens
  * Node Name: `http://host.docker.internal:8081/api/biens ()({adresse,ville,type,surface,prixMensuel,charges})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `Access-Control-Allow-Origin: *`
  * Other Info: `The CORS misconfiguration on the web server permits cross-domain read requests from arbitrary third party domains, using unauthenticated APIs on this domain. Web browser implementations do not permit arbitrary third parties to read the response from authenticated APIs, however. This reduces the risk somewhat. This misconfiguration could be used by an attacker to access data that is available in an unauthenticated manner, but which uses some other form of security, such as IP address white-listing.`

Instances: Systemic


### Solution

Ensure that sensitive data is not available in an unauthenticated manner (using IP address white-listing, for instance).
Configure the "Access-Control-Allow-Origin" HTTP header to a more restrictive set of domains, or remove all CORS headers entirely, to allow the web browser to enforce the Same Origin Policy (SOP) in a more restrictive manner.

### Reference


* [ https://vulncat.fortify.com/en/detail?category=HTML5&subcategory=Overly%20Permissive%20CORS%20Policy ](https://vulncat.fortify.com/en/detail?category=HTML5&subcategory=Overly%20Permissive%20CORS%20Policy)


#### CWE Id: [ 264 ](https://cwe.mitre.org/data/definitions/264.html)


#### WASC Id: 14

#### Source ID: 3

### [ Server Leaks Information via "X-Powered-By" HTTP Response Header Field(s) ](https://www.zaproxy.org/docs/alerts/10037/)



##### Low (Medium)

### Description

The web/application server is leaking information via one or more "X-Powered-By" HTTP response headers. Access to such information may facilitate attackers identifying other frameworks/components your web application is reliant upon and the vulnerabilities such components may be subject to.

* URL: http://host.docker.internal:8081/api/biens/id
  * Node Name: `http://host.docker.internal:8081/api/biens/id`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `X-Powered-By: Express`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens%3Fville=ville&statut=LIBRE&type=type
  * Node Name: `http://host.docker.internal:8081/api/biens (statut,type,ville)`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `X-Powered-By: Express`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/statut
  * Node Name: `http://host.docker.internal:8081/api/biens/id/statut ()({statut})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `X-Powered-By: Express`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens
  * Node Name: `http://host.docker.internal:8081/api/biens ()({adresse,ville,type,surface,prixMensuel,charges})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `X-Powered-By: Express`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id
  * Node Name: `http://host.docker.internal:8081/api/biens/id ()({adresse,ville,type,surface,prixMensuel,charges,statut})`
  * Method: `PUT`
  * Parameter: ``
  * Attack: ``
  * Evidence: `X-Powered-By: Express`
  * Other Info: ``

Instances: Systemic


### Solution

Ensure that your web server, application server, load balancer, etc. is configured to suppress "X-Powered-By" headers.

### Reference


* [ https://owasp.org/www-project-web-security-testing-guide/v42/4-Web_Application_Security_Testing/01-Information_Gathering/08-Fingerprint_Web_Application_Framework ](https://owasp.org/www-project-web-security-testing-guide/v42/4-Web_Application_Security_Testing/01-Information_Gathering/08-Fingerprint_Web_Application_Framework)
* [ https://www.troyhunt.com/shhh-dont-let-your-response-headers/ ](https://www.troyhunt.com/shhh-dont-let-your-response-headers/)


#### CWE Id: [ 497 ](https://cwe.mitre.org/data/definitions/497.html)


#### WASC Id: 13

#### Source ID: 3

### [ X-Content-Type-Options Header Missing ](https://www.zaproxy.org/docs/alerts/10021/)



##### Low (Medium)

### Description

The Anti-MIME-Sniffing header X-Content-Type-Options was not set to 'nosniff'. This allows older versions of Internet Explorer and Chrome to perform MIME-sniffing on the response body, potentially causing the response body to be interpreted and displayed as a content type other than the declared content type. Current (early 2014) and legacy versions of Firefox will use the declared content type (if one is set), rather than performing MIME-sniffing.

* URL: http://host.docker.internal:8081/health
  * Node Name: `http://host.docker.internal:8081/health`
  * Method: `GET`
  * Parameter: `x-content-type-options`
  * Attack: ``
  * Evidence: ``
  * Other Info: `This issue still applies to error type pages (401, 403, 500, etc.) as those pages are often still affected by injection issues, in which case there is still concern for browsers sniffing pages away from their actual content type.
At "High" threshold this scan rule will not alert on client or server error responses.`


Instances: 1

### Solution

Ensure that the application/web server sets the Content-Type header appropriately, and that it sets the X-Content-Type-Options header to 'nosniff' for all web pages.
If possible, ensure that the end user uses a standards-compliant and modern web browser that does not perform MIME-sniffing at all, or that can be directed by the web application/web server to not perform MIME-sniffing.

### Reference


* [ https://learn.microsoft.com/en-us/previous-versions/windows/internet-explorer/ie-developer/compatibility/gg622941(v=vs.85) ](https://learn.microsoft.com/en-us/previous-versions/windows/internet-explorer/ie-developer/compatibility/gg622941(v=vs.85))
* [ https://owasp.org/www-community/Security_Headers ](https://owasp.org/www-community/Security_Headers)


#### CWE Id: [ 693 ](https://cwe.mitre.org/data/definitions/693.html)


#### WASC Id: 15

#### Source ID: 3

### [ A Client Error response code was returned by the server ](https://www.zaproxy.org/docs/alerts/100000/)



##### Informational (High)

### Description

A response code of 401 was returned by the server.
This may indicate that the application is failing to handle unexpected input correctly.
Raised by the 'Alert on HTTP Response Code Error' script

* URL: http://host.docker.internal:8081/api/biens/id
  * Node Name: `http://host.docker.internal:8081/api/biens/id`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/
  * Node Name: `http://host.docker.internal:8081/api/biens/id/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/computeMetadata/v1/
  * Node Name: `http://host.docker.internal:8081/computeMetadata/v1/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/latest/meta-data/
  * Node Name: `http://host.docker.internal:8081/latest/meta-data/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/metadata/instance
  * Node Name: `http://host.docker.internal:8081/metadata/instance`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/metadata/v1
  * Node Name: `http://host.docker.internal:8081/metadata/v1`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/opc/v1/instance/
  * Node Name: `http://host.docker.internal:8081/opc/v1/instance/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/opc/v2/instance/
  * Node Name: `http://host.docker.internal:8081/opc/v2/instance/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/openstack/latest/meta_data.json
  * Node Name: `http://host.docker.internal:8081/openstack/latest/meta_data.json`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081
  * Node Name: `http://host.docker.internal:8081`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/
  * Node Name: `http://host.docker.internal:8081/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/5721729060123990889
  * Node Name: `http://host.docker.internal:8081/5721729060123990889`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api
  * Node Name: `http://host.docker.internal:8081/api`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/
  * Node Name: `http://host.docker.internal:8081/api/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/6350560496820131127
  * Node Name: `http://host.docker.internal:8081/api/6350560496820131127`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens
  * Node Name: `http://host.docker.internal:8081/api/biens`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens%3Fville=ville&statut=LIBRE&type=type
  * Node Name: `http://host.docker.internal:8081/api/biens (statut,type,ville)`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/
  * Node Name: `http://host.docker.internal:8081/api/biens/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/2378667680283554681
  * Node Name: `http://host.docker.internal:8081/api/biens/2378667680283554681`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/actuator/health
  * Node Name: `http://host.docker.internal:8081/api/biens/actuator/health`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id
  * Node Name: `http://host.docker.internal:8081/api/biens/id`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/
  * Node Name: `http://host.docker.internal:8081/api/biens/id/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/1813480600924232141
  * Node Name: `http://host.docker.internal:8081/api/biens/id/1813480600924232141`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/disponibilite
  * Node Name: `http://host.docker.internal:8081/api/biens/id/disponibilite`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/disponibilite/
  * Node Name: `http://host.docker.internal:8081/api/biens/id/disponibilite/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/statut
  * Node Name: `http://host.docker.internal:8081/api/biens/id/statut ()({statut})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/statut/
  * Node Name: `http://host.docker.internal:8081/api/biens/id/statut/ ()({statut})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens
  * Node Name: `http://host.docker.internal:8081/api/biens ()({adresse,ville,type,surface,prixMensuel,charges})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/
  * Node Name: `http://host.docker.internal:8081/api/biens/ ()({adresse,ville,type,surface,prixMensuel,charges})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id
  * Node Name: `http://host.docker.internal:8081/api/biens/id ()({adresse,ville,type,surface,prixMensuel,charges,statut})`
  * Method: `PUT`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/
  * Node Name: `http://host.docker.internal:8081/api/biens/id/ ()({adresse,ville,type,surface,prixMensuel,charges,statut})`
  * Method: `PUT`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``


Instances: 31

### Solution



### Reference



#### CWE Id: [ 388 ](https://cwe.mitre.org/data/definitions/388.html)


#### WASC Id: 20

#### Source ID: 4

### [ Non-Storable Content ](https://www.zaproxy.org/docs/alerts/10049/)



##### Informational (Medium)

### Description

The response contents are not storable by caching components such as proxy servers. If the response does not contain sensitive, personal or user-specific information, it may benefit from being stored and cached, to improve performance.

* URL: http://host.docker.internal:8081/api/biens%3Fville=ville&statut=LIBRE&type=type
  * Node Name: `http://host.docker.internal:8081/api/biens (statut,type,ville)`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id
  * Node Name: `http://host.docker.internal:8081/api/biens/id`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id/disponibilite
  * Node Name: `http://host.docker.internal:8081/api/biens/id/disponibilite`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens
  * Node Name: `http://host.docker.internal:8081/api/biens ()({adresse,ville,type,surface,prixMensuel,charges})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `401`
  * Other Info: ``
* URL: http://host.docker.internal:8081/api/biens/id
  * Node Name: `http://host.docker.internal:8081/api/biens/id ()({adresse,ville,type,surface,prixMensuel,charges,statut})`
  * Method: `PUT`
  * Parameter: ``
  * Attack: ``
  * Evidence: `PUT `
  * Other Info: ``

Instances: Systemic


### Solution

The content may be marked as storable by ensuring that the following conditions are satisfied:
The request method must be understood by the cache and defined as being cacheable ("GET", "HEAD", and "POST" are currently defined as cacheable)
The response status code must be understood by the cache (one of the 1XX, 2XX, 3XX, 4XX, or 5XX response classes are generally understood)
The "no-store" cache directive must not appear in the request or response header fields
For caching by "shared" caches such as "proxy" caches, the "private" response directive must not appear in the response
For caching by "shared" caches such as "proxy" caches, the "Authorization" header field must not appear in the request, unless the response explicitly allows it (using one of the "must-revalidate", "public", or "s-maxage" Cache-Control response directives)
In addition to the conditions above, at least one of the following conditions must also be satisfied by the response:
It must contain an "Expires" header field
It must contain a "max-age" response directive
For "shared" caches such as "proxy" caches, it must contain a "s-maxage" response directive
It must contain a "Cache Control Extension" that allows it to be cached
It must have a status code that is defined as cacheable by default (200, 203, 204, 206, 300, 301, 404, 405, 410, 414, 501).

### Reference


* [ https://datatracker.ietf.org/doc/html/rfc7234 ](https://datatracker.ietf.org/doc/html/rfc7234)
* [ https://datatracker.ietf.org/doc/html/rfc7231 ](https://datatracker.ietf.org/doc/html/rfc7231)
* [ https://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html ](https://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html)


#### CWE Id: [ 524 ](https://cwe.mitre.org/data/definitions/524.html)


#### WASC Id: 13

#### Source ID: 3

### [ Storable and Cacheable Content ](https://www.zaproxy.org/docs/alerts/10049/)



##### Informational (Medium)

### Description

The response contents are storable by caching components such as proxy servers, and may be retrieved directly from the cache, rather than from the origin server by the caching servers, in response to similar requests from other users. If the response data is sensitive, personal or user-specific, this may result in sensitive information being leaked. In some cases, this may even result in a user gaining complete control of the session of another user, depending on the configuration of the caching components in use in their environment. This is primarily an issue where "shared" caching servers such as "proxy" caches are configured on the local network. This configuration is typically found in corporate or educational environments, for instance.

* URL: http://host.docker.internal:8081/health
  * Node Name: `http://host.docker.internal:8081/health`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: ``
  * Other Info: `In the absence of an explicitly specified caching lifetime directive in the response, a liberal lifetime heuristic of 1 year was assumed. This is permitted by rfc7234.`


Instances: 1

### Solution

Validate that the response does not contain sensitive, personal or user-specific information. If it does, consider the use of the following HTTP response headers, to limit, or prevent the content being stored and retrieved from the cache by another user:
Cache-Control: no-cache, no-store, must-revalidate, private
Pragma: no-cache
Expires: 0
This configuration directs both HTTP 1.0 and HTTP 1.1 compliant caching servers to not store the response, and to not retrieve the response (without validation) from the cache, in response to a similar request.

### Reference


* [ https://datatracker.ietf.org/doc/html/rfc7234 ](https://datatracker.ietf.org/doc/html/rfc7234)
* [ https://datatracker.ietf.org/doc/html/rfc7231 ](https://datatracker.ietf.org/doc/html/rfc7231)
* [ https://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html ](https://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html)


#### CWE Id: [ 524 ](https://cwe.mitre.org/data/definitions/524.html)


#### WASC Id: 13

#### Source ID: 3


