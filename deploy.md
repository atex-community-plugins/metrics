Metrics and Health Checks plugin
================================

This is the deploy guide.

You need to use java 1.7

### (Re)Deploy an existing release

```
mvn clean install deploy -Pdeploy
```

### Prepare for a release

```
mvn clean install deploy release:prepare -Pdeploy -DautoVersionSubmodules=true
mvn clean install deploy release:perform -Pdeploy
git commit -a -m "prepare for next development"
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

