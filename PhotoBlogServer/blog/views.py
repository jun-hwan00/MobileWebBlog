from django.shortcuts import render
from rest_framework import viewsets
from .models import Post
from .serializers import PostSerializer
from asgiref.sync import async_to_sync
from channels.layers import get_channel_layer  # ← 줄바꿈!

class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
    
    def perform_create(self, serializer):
        # 게시물 저장
        post = serializer.save()
        
        # WebSocket으로 알림 전송
        channel_layer = get_channel_layer()
        async_to_sync(channel_layer.group_send)(
            'notifications',
            {
                'type': 'detection_alert',
                'message': {
                    'type': 'detection_alert',
                    'object_name': post.title or 'Unknown',
                    'confidence': 0.85,
                    'timestamp': post.created_date.isoformat(),
                    'image_id': post.id,
                    'image_url': post.image.url if post.image else None
                }
            }
        )

def post_list(request):
    posts = Post.objects.all()
    return render(request, 'blog/post_list.html', {'posts': posts})