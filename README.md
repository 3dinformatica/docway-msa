# MSA




## BUILD
-compilare da Eclipse
-Posizionarsi nella home del progetto (Es: 'user/git/it.tredi.msa') ed eseguire il comando:
	mvn package
-Prelevare il file 'target/msa-<VERSION_NUMBER>.jar' e decomprimerlo
 
## START
-Posizionarsi nella cartella 'msa-<VERSION_NUMBER>' ed eseguire il comando
	java -Xmx1024m org.springframework.boot.loader.JarLauncher
	
	
