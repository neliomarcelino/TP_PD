cd ..\..\Servidor\
call mvn exec:java -Dexec.mainClass=pt.isec.tppd.g24.servidor.Main -Dexec.args="5003 5004 127.0.0.1/servidor2"
pause
