from rest_framework import viewsets
from .models import Post
from .serializers import PostSerializer
from django.shortcuts import render
class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
    
def post_list(request):
    """블로그 형태로 게시물 목록 표시"""
    posts = Post.objects.all().order_by('-published_date')
    return render(request, 'blog/post_list.html', {'posts': posts})