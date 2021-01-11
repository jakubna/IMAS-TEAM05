## IMAS Fuzzy Agent-Based Decision Support System

### How to run

This project is intended to be run using IntelliJ IDEA, with Java 8.

The project already comes with a run configuration (called *jade*) ready to execute the application.
In any case, the CLI arguments are `-gui -agents user:UsrAgent;manager:MngrAgent`

Then, the application menu provides the instructions necessary to execute each action.

 \
Finally, in some cases, the application might not start properly (the menu does not even show). The solution to that problem is to manually configure the jade dependency to use the locally installed jade library.

In IntelliJ: *File* > *Project Structure...* > *Project Settings* > *Libraries*. Then add the local *jade.jar*  