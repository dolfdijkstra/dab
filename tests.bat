@echo off
set t=600

FOR %%A IN (1 4 16 ) DO ( 
	rem echo java -jar target\dab-jar-with-dependencies.jar -c %%A -t %t% -r 4 "http://jsk-virtualbox/home.html"
	rem echo java -jar target\dab-jar-with-dependencies.jar -c %%A -t %t% -r 4 "http://jsk-virtualbox:8180/cs/home.jsp"

	FOR %%S IN (437 8192 30720 122880) DO ( 
	 echo java -jar target\dab-jar-with-dependencies.jar -c %%A -t %t% -r 4 "http://jsk-virtualbox/lorem-%%S.txt"
	 echo java -jar target\dab-jar-with-dependencies.jar -c %%A -t %t% -r 4 "http://jsk-virtualbox:8180/cs/lorem.jsp?size=%%S" 
	 echo java -jar target\dab-jar-with-dependencies.jar -c %%A -t %t% -r 4 "http://jsk-virtualbox:8180/cs/ContentServer?pagename=Support/Performance/Standard/lorem&size=%%S" 
	 echo java -jar target\dab-jar-with-dependencies.jar -c %%A -t %t% -r 4 "http://jsk-virtualbox:8180/cs/Satellite?pagename=Support/Performance/Standard/lorem&size=%%S" 
	 echo java -jar target\dab-jar-with-dependencies.jar -c %%A -t %t% -r 4 "http://jsk-virtualbox:8181/cs/Satellite?pagename=Support/Performance/Standard/lorem&size=%%S" 

	)
)
   
