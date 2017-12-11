Metrics and Health Checks plugin
================================

This is the deploy guide.

You need to use java 1.7

### (Re)Deploy an existing release

```
mvn clean install deploy -Pdeploy
```

### Prepare for a release

First you need to generate the release notes, use the last tag generated:

```
./getReleaseNotes.sh  metrics-xxxx | pbcopy
```

Edit the file `releaseNotes.md` by adding the new release on top and pasting the output of the script above,
update `README.md` with the version that will be released (i.e. in the dependency examples) and then push to git.

```
git commit -a -m "update release notes"
git push
```

```
mvn clean install deploy release:prepare -Pdeploy -DautoVersionSubmodules=true
mvn clean install deploy release:perform -Pdeploy
```

### Cleanup a broken release

Sometimes when you perform a release it may fails with an error `Unable to tag SCM`.

You need to clean the release

```
mvn release:clean
```

and remove the tag from the local and remote git:

```
git tag -d metrics-xxxx
git push --delete origin metrics-xxxx
```

finally you can cleanup the checked out files (this will revert all the modified files!!!):

```
git reset HEAD --hard
```

Then you can try to release it again.

