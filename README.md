# Asynchronous Database Connectivity in Java

This is a project I created for my [thesis](http://scholarsarchive.byu.edu/cgi/viewcontent.cgi?article=3386&context=etd)
at [Brigham Young University](http://byu.edu). There are four maincomponents to this project:

1. A database agnostic API (conceptually similar to JDBC) for asynchronous/non-blocking relational database interaction.
2. A MySQL native implemention of the API
3. A Postgresql native implementation of the API
4. An API implementation that uses JDBC and and a thread-pool for asynchrony.

These database drivers are not production worthy.
