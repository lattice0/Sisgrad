# Sisgrad
Sisgrad is the Unesp's main system for sending messages, sharing informations about students and to inform people about the university activities.
'Sisgrad' is an android app that connects to Unesp's Sisgrad, Parthenon and Lattes, which are the most used systems in the university. 

#Build
Be sure to have android-sdk installed, then you have to create the file local.properties at /Sisgrad containing the path to the sdk. For example, my sdk is at /home/lucaszanella/Android/Sdk so I did:
```
git clone https://github.com/lucaszanella/Sisgrad
cd Sisgrad
nano local.properties
```
and wrote: 
```
sdk.dir=/home/lucaszanella/Android/Sdk
```
Now, to build the project, just run
```
./gradlew build
```
at /Sisgrad, it'll compile using the included gradlew (gradle wrapper). If you want to build with your own gradle, just run gradle build, but note that it's better to build with the wrapper.

#Privacy
As you can see in the source code, user's information never leaves the app, unless they're going to the official servers. HTTPs is used throughout the entire Sisgrad module. However, this was not possible with Lattes, which only support HTTP unencrypted connection. The good part is that Lattes doesn't require any authentication, it's simply for searching about people's academic life. Parthenon supports HTTPs only in the authentication form. Not bad, but cookies can be seen in plain text. Since people already connect to the system in this way everytime, I'm supporting Parthenon in this app, because the information that can be leaked is unharmful (or not...).

#Contribution
I accept ANY kind of help, so anyone can send patches, suggestions and open issues. I also accept input about my programming style and how I can make it better. If you're from Sisgrad, Parthenon or Lattes and you're reading this, I'd like to ask you to implement a simple json API so the app can work between changes in Sisgrad's HTML layout.

