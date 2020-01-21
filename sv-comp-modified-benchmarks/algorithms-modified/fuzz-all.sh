root=$(pwd)
echo "" > "$root""/complete-result.txt"
for d in */ ; do
	current="$root""/""$d"
	SECONDS=0
	cd $current && javac -cp .:$(../../../scripts/classpath.sh) *.java && timeout 600 ../../../bin/jqf-zest -c .:$(../../../scripts/classpath.sh) IntegerTest test && echo "$d"" is complete!"
	duration=$SECONDS
	echo "$(($duration / 60)) minutes and $(($duration % 60)) seconds elapsed." &&
	echo "$current""fuzz-result.txt"
	echo "$d"" is complete" >> ../complete-result.txt &&
	cat "$current""fuzz-result.txt" >> ../complete-result.txt &&
	echo -e "\n" >> ../complete-result.txt &&
	echo "$(($duration / 60)) minutes and $(($duration % 60)) seconds elapsed." >> ../complete-result.txt
	echo -e "\n-----------------------------\n" >> ../complete-result.txt
done