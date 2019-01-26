package com.centaur.plugins

import grails.plugins.*
import grails.util.GrailsNameUtils
import com.centaur.plugins.Comment
import com.centaur.plugins.CommentException
import com.centaur.plugins.CommentLink

class CommentableGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.8 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Commentable" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    Closure doWithSpring() { {->
            // TODO Implement runtime spring config (optional)
        }
    }

    void doWithDynamicMethods() {
        for(domainClass in grailsApplication.domainClasses) {
            if(com.centaur.plugins.Commentable.class.isAssignableFrom(domainClass.clazz)) {
                domainClass.clazz.metaClass {
                    'static' {
                        getRecentComments {->
                            def clazz = delegate
                            com.centaur.plugins.CommentLink.withCriteria {
                                projections { property "comment" }
                                eq 'type', GrailsNameUtils.getPropertyName(clazz)
                                maxResults 5
                                cache true
                                comment {
                                    order "dateCreated", "desc"
                                }
                            }
                        }
                    }

                    addComment { poster, String text ->
                        if(delegate.id == null) throw new CommentException("You must save the entity [${delegate}] before calling addComment")

                        def posterClass = poster.class.name
                        def i = posterClass.indexOf('_$$_javassist')
                        if(i>-1)
                            posterClass = posterClass[0..i-1]

                        def c = new Comment(body:text, posterId:poster.id, posterClass:posterClass)
                        if(!c.validate()) {
                            throw new CommentException("Cannot create comment for arguments [$poster, $text], they are invalid.")
                        }
                        c.save()
                        def link = new CommentLink(comment:c, commentRef:delegate.id, type:GrailsNameUtils.getPropertyName(delegate.class))
                        link.save()
                        try {
                            delegate.onAddComment(c)
                        } catch (MissingMethodException e) {}
                        return delegate
                    }

                    getComments = {->
                        def instance = delegate
                        if(instance.id != null) {
                            CommentLink.withCriteria {
                                projections {
                                    property "comment"
                                }
                                eq "commentRef", instance.id
                                eq 'type', GrailsNameUtils.getPropertyName(instance.class)
                                cache true
                            }
                        } else {
                            return Collections.EMPTY_LIST
                        }
                    }

                    getTotalComments = {->
                        def instance = delegate
                        if(instance.id != null) {
                            CommentLink.createCriteria().get {
                                projections {
                                    rowCount()
                                }
                                eq "commentRef", instance.id
                                eq 'type', GrailsNameUtils.getPropertyName(instance.class)
                                cache true
                            }
                        } else {
                            return 0
                        }
                    }

                    removeComment { Comment c ->
                        CommentLink.findAllByComment(c)*.delete()
                        c.delete(flush:true) // cascades deletes to links
                    }

                    removeComment { Long id ->
                        def c = Comment.get(id)
                        if(c) removeComment(c)
                    }
                }
            }
        }
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
