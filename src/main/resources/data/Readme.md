The first and last names are from `https://github.com/dominictarr/random-name/`, the city and street names are from `https://github.com/danielmiessler/SecLists`: 

```
jshell> Files.writeString(Path.of("firstNames.json"), new BufferedReader(new InputStreamReader(new URL("https://raw.githubusercontent.com/dominictarr/random-name/master/first-names.txt").openStream())).lines().sorted().reduce("", (a, b) -> { if (a.length() == 0) return "[\n\"" + b + "\""; else return a + ",\n\"" + b + "\"";}) + "\n]\n")

jshell> Files.writeString(Path.of("lastNames.json"), new BufferedReader(new InputStreamReader(new URL("https://raw.githubusercontent.com/dominictarr/random-name/master/names.txt").openStream())).lines().sorted().reduce("", (a, b) -> { if (a.length() == 0) return "[\n\"" + b + "\""; else return a + ",\n\"" + b + "\"";}) + "\n]\n")

Files.writeString(Path.of("cities.json"), new BufferedReader(new InputStreamReader(new URL("https://raw.githubusercontent.com/danielmiessler/SecLists/master/Miscellaneous/us-cities.txt").openStream())).lines().sorted().reduce("", (a, b) -> { if (a.length() == 0) return "[\n\"" + b + "\""; else return a + ",\n\"" + b + "\"";}) + "\n]\n")

Files.writeString(Path.of("streets.json"), new BufferedReader(new InputStreamReader(new URL("https://raw.githubusercontent.com/danielmiessler/SecLists/master/Miscellaneous/security-question-answers/street-names.txt").openStream())).lines().filter(l -> !Character.isDigit(l.charAt(0)) && !Character.isLowerCase(l.charAt(0))).sorted().reduce("", (a, b) -> { if (a.length() == 0) return "[\n\"" + b + "\""; else return a + ",\n\"" + b + "\"";}) + "\n]\n")
```
