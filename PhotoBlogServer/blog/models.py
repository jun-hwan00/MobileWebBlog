from django.db import models
from django.contrib.auth.models import User

class Post(models.Model):
    author = models.ForeignKey(User, on_delete=models.CASCADE)
    title = models.CharField(max_length=200, blank=True, default='')  # 선택사항으로
    text = models.TextField(blank=True, default='')  # 선택사항으로
    created_date = models.DateTimeField(auto_now_add=True)
    published_date = models.DateTimeField(auto_now=True)
    image = models.ImageField(upload_to='blog_image/%Y/%m/%d/')
    
    def __str__(self):
        return self.title if self.title else f"Post {self.id}"