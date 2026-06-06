Here is the step-by-step guide to manually configure Keycloak via its Admin Console to run the application, without relying on the JSON realm import.
________________________________________
Step 1: Create the Realm
1.	Open the Keycloak Admin Console (typically at http://localhost:8080/admin).
2.	Log in with your admin credentials (e.g. username admin and password admin).
3.	Click the dropdown in the top-left corner and select Create Realm.
4.	Set the Realm Name to certmanager-realm and click Create.
________________________________________
Step 2: Create the Frontend Client
1.	Navigate to Clients in the left menu.
2.	Click Create client.
3.	Configure the following:
•	Client type: OpenID Connect
•	Client ID: certmanager-frontend
•	Name: CertManager Frontend
•	Click Next.
4.	In the Capabilities configuration:
•	Client authentication: OFF (Makes it a public client).
•	Authorization: OFF.
•	Standard flow: ON (Checked).
•	Implicit flow & Direct access grants: OFF (Unchecked).
•	Click Next.
5.	Configure Login settings:
•	Valid Redirect URIs:
•	http://localhost:5173/* (for Vite development)
•	http://localhost:3000/* (for production/Nginx container)
•	Web Origins:
•	http://localhost:5173
•	http://localhost:3000
•	Click Save.
6.	Under the client's Advanced settings:
•	Verify that Proof Key for Code Exchange (PKCE) Challenge Method is set to S256.
________________________________________
Step 3: Configure Client Scopes & Mappers (Crucial)
To pass user group paths, roles, and permission claims inside the JWT tokens, we need to map them under our client's dedicated scopes.
1.	Inside your certmanager-frontend client page, select the Client Scopes tab.
2.	Click the dedicated scope (named certmanager-frontend-dedicated).
3.	Select Add mapper -> By configuration.
Add the following three mappers:
Mapper 1: Groups (Groups Mapper)
•	Mapper type: Group Membership
•	Name: groups-mapper
•	Token Claim Name: groups
•	Full path: ON (Very important: this maps /admins instead of just admins).
•	Add to ID token: ON.
•	Add to access token: ON.
•	Add to userinfo: ON.
•	Click Save.
Mapper 2: Permissions (Realm Roles Mapper)
•	Mapper type: User Realm Role
•	Name: permissions-mapper
•	Token Claim Name: permissions
•	Multivalued: ON.
•	Add to ID token: ON.
•	Add to access token: ON.
•	Add to userinfo: ON.
•	Claim JSON Type: String.
•	Click Save.
Mapper 3: Access Roles (Realm Roles Mapper for API Security)
•	Mapper type: User Realm Role
•	Name: realm-roles-mapper
•	Token Claim Name: realm_access.roles
•	Multivalued: ON.
•	Add to ID token: OFF.
•	Add to access token: ON.
•	Add to userinfo: OFF.
•	Claim JSON Type: String.
•	Click Save.
________________________________________
Step 4: Create Realm Roles
1.	Navigate to Realm roles in the left menu.
2.	Click Create role.
3.	Create the following roles:
•	ROLE_ADMIN (Administrator role)
•	ROLE_USER (Standard user role)
•	READ (Permission to read)
•	WRITE (Permission to write/upload)
•	UPDATE (Permission to update metadata)
________________________________________
Step 5: Create User Groups
1.	Navigate to Groups in the left menu.
2.	Click Create group.
3.	Create three groups (paths will automatically be prefixed with /):
•	admins (resolves as /admins)
•	developers (resolves as /developers)
•	viewers (resolves as /viewers)
________________________________________
Step 6: Create Users
Navigate to Users in the left menu, click Add user, and configure the users as follows:
User 1: Administrator (admin)
•	Username: admin
•	Email: admin@certmanager.local
•	First Name: Admin
•	Last Name: User
•	Click Create.
•	Groups: Join the /admins group.
•	Role mapping: Assign the realm roles ROLE_ADMIN, READ, WRITE, and UPDATE.
•	Credentials: Click Set password, enter Admin@123, and turn Temporary OFF.
User 2: Developer (dev1)
•	Username: dev1
•	Email: dev1@certmanager.local
•	First Name: Developer
•	Last Name: One
•	Click Create.
•	Groups: Join the /developers group.
•	Role mapping: Assign the realm roles ROLE_ADMIN, READ, and WRITE.
•	Credentials: Click Set password, enter Dev@1234, and turn Temporary OFF.
User 3: Viewer (user1)
•	Username: user1
•	Email: user1@certmanager.local
•	First Name: Regular
•	Last Name: User
•	Click Create.
•	Groups: Join the /viewers group.
•	Role mapping: Assign the realm roles ROLE_USER and READ.
•	Credentials: Click Set password, enter User@1234, and turn Temporary OFF.
3:55 PM
i want to test with bruno api the login what should do ?
4:51 PM
To test your secured backend APIs using Bruno, you need to fetch an access token from Keycloak first and then attach it as a Bearer token to your API requests.
Here are the two ways to set this up in Bruno.
________________________________________
Method A: Fetch Token Manually (Using a POST Request)
This is the most transparent way to see the token payload directly.
Step 1: Create a Request to Fetch the Token
1.	In Bruno, create a new request:
•	Method: POST
•	URL: http://localhost:8080/realms/certmanager-realm/protocol/openid-connect/token
2.	Go to the Body tab and select Form (x-www-form-urlencoded).
3.	Add the following key-value pairs:
•	client_id: certmanager-frontend
•	grant_type: password
•	username: admin (or user1 / dev1)
•	password: Admin@123 (or the respective password)
•	scope: openid profile email roles groups
4.	Send the request. Keycloak will return a JSON body. Copy the value of the access_token field.



Method B: Configure Bruno's Built-in OAuth2 (Automated & Recommended)
Bruno can fetch and renew the token automatically so you don't have to keep copying and pasting.
Step 1: Configure Collection-Level OAuth2
1.	Right-click your collection folder in Bruno and select Settings (or click the Auth tab on the request level).
2.	Select the Auth tab and choose OAuth2 from the dropdown.
3.	Configure the OAuth2 settings as follows:
•	Grant Type: Password Credentials
•	Token URL: http://localhost:8080/realms/certmanager-realm/protocol/openid-connect/token
•	Username: admin
•	Password: Admin@123
•	Client ID: certmanager-frontend
•	Scope: openid profile email roles groups
4.	Click Get Access Token. Bruno will handle the request in the background and display the retrieved token.
Step 2: Configure Your API Request
1.	Go to your API request (e.g. GET http://localhost:8081/api/v1/certificates).
2.	Go to the Auth tab of the request and select Inherit (or OAuth2).
3.	Send the request. Bruno will automatically inject the Authorization: Bearer <token> header for you!
The error Client not allowed for direct access grants means Keycloak is blocking direct username/password token requests (grant_type=password) for the certmanager-frontend client.
To allow this flow so you can test it with curl or Bruno, you just need to enable the Direct Access Grants capability in Keycloak:
How to enable it:
1.	Open the Keycloak Admin Console and select the certmanager-realm realm.
2.	Navigate to Clients in the left menu and click on certmanager-frontend.
3.	Under the Capability config section (on the main Settings page):
•	Find the check box or toggle for Direct access grants (also called Direct Grant Flow).
•	Turn it ON (Checked).
4.	Scroll to the bottom and click Save.




