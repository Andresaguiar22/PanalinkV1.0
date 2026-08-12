import sys

with open('app/src/main/java/com/example/data/supabase/SupabaseApiService.kt', 'r') as f:
    content = f.read()

share_api = """    @POST("rest/v1/post_shares")
    suspend fun addShare(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body share: com.example.data.model.PostShareDto
    ): retrofit2.Response<okhttp3.ResponseBody>

"""

# find @DELETE("rest/v1/post_likes") and insert before it
content = content.replace(
    '    @DELETE("rest/v1/post_likes")',
    share_api + '    @DELETE("rest/v1/post_likes")'
)

with open('app/src/main/java/com/example/data/supabase/SupabaseApiService.kt', 'w') as f:
    f.write(content)
