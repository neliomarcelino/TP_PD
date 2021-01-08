cd ..\..\Servidor\
call mvn exec:java -Dexec.mainClass=pt.isec.tppd.g24.servidor.Main -Dexec.args="5005 5006 127.0.0.1/servidor3"
pause