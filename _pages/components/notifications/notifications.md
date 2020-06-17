---
layout: inner
title: Notifications
lead_text: ''
permalink: /components/notifications/
---

# Notifications and Curation for Smart Mastering

1. Notifications Pane 
	* An inbox alerting users which records have been identified as similar by Smart Mastering.
2. Merge Curation and History Pane 
	* Choose to merge or block the matches found from being merged for the similar records identified.  
	* View which documents have been merged to create the mastered document.

### Getting Started

Assuming you have set the action in the smart mastering Match step of your data hub flow to "notify" and the flow has been run,  an alert for how many matching records have been identified will be reported in the upper right of Envision next to the user name after login.

![Alerts](/envision/images/notifications-alert.png){: .center-image }


Simply navigate from the dropdown menu in the upper right to "Notifications" to view the matches to be curated.

![Menu Option](/envision/images/notifications-menu.png){: .center-image }

In the Notifications Inbox, choose the record to curate.

![Notification Inbox](/envision/images/notifications-pane.png)

The Merge Curation and History screen allows you to view the similar records for the notification.  The Flow and Step are reported along with the Entity type that was matched.   Here you can decide whether to merge the records, or block them from matching and merging if this step is ran again in the future in your data hub.

![Merge Curation and History](/envision/images/merge-curation.png)

The merge preview label will be replaced by the URI for the merged record in case of merging.

Entities in the Explore pane that are the result of merged records provide the ability to navigate to this Merge History from  Explore.

