package ehr

import directory.Folder

class EhrTagLib {

   def hasEhr = { attrs, body ->
      
      if (!attrs.patientUID) throw new Exception("patientUID es obligatorio")
      
      //println patientUID
      
      def c = Ehr.createCriteria()
      
      def ehr = c.get {
         subject {
            eq ('value', attrs.patientUID)
         }
      }
      
      //println ehr
      
      if (ehr) out << body()
   }
   
   def dontHasEhr = { attrs, body ->

      if (!attrs.patientUID) throw new Exception("patientUID es obligatorio")
      
      //println attrs.patientUID
      
      def c = Ehr.createCriteria()
      
      def ehr = c.get {
         subject {
            eq ('value', attrs.patientUID)
         }
      }
      
      //println ehr
      
      if (!ehr) out << body()
   }
   
   def ehr_directory = { attrs, body ->
      
      if (!attrs.directory) return
      
      out << recursive_directory(attrs.directory)
   }
   
   private String recursive_directory(Folder folder)
   {
      def html = '<div class="folder"><div class="folder_name">'+ folder.name +'('+ folder.items.size() +')</div><div class="folder_folders">'
      
      folder.folders.each {
         html += recursive_directory(it)
      }
      
      html += '</div></div>'
      
      return html
   }
}