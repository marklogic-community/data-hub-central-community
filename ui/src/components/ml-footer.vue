<template>
	<v-footer
     color="transparent">
    <v-container mx-auto>
    <v-layout row class="footer">
				<div>
                  <span>© {{currentYear}} MarkLogic Corporation</span>
                  |
                  <span>
                    <a href="https://www.marklogic.com/privacy/" class="link">Privacy</a>
                  </span>
          </div>
				</v-layout>
    </v-container>
  </v-footer>
</template>

<script>
export default {
  name: 'ml-footer',
  data() {
    return {
      currentYear: new Date().getUTCFullYear()
    };
  }
};
</script>

<style>
.footer{
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 14px;
    color: rgb(153, 153, 153);
 }

 .link{
  color: rgb(68, 73, 156);
  text-decoration: none;
 }

 .link:hover{
  color: #7fade3;
 }
</style>

