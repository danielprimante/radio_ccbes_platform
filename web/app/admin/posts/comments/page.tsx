'use client';

import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { getCommentsForPost, deleteComment, updateComment, type Comment } from '@/lib/api';

function PostCommentsContent() {
    const searchParams = useSearchParams();
    const postId = searchParams.get('id');
    const postContent = searchParams.get('content') || '';

    const [user, setUser] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [comments, setComments] = useState<Comment[]>([]);
    const router = useRouter();

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
            if (!currentUser) {
                router.push('/login');
            } else {
                setUser(currentUser);
                if (postId) {
                    loadComments(postId);
                } else {
                    // If no postId, maybe just go back?
                    // router.push('/admin'); 
                }
            }
        });

        return () => unsubscribe();
    }, [router, postId]);

    const loadComments = async (id: string) => {
        try {
            const data = await getCommentsForPost(id);
            setComments(data);
        } catch (error) {
            console.error('Error loading comments:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (commentId: string) => {
        if (!confirm('¿Estás seguro de eliminar este comentario?')) return;

        try {
            await deleteComment(commentId);
            if (postId) loadComments(postId);
        } catch (error) {
            console.error('Error deleting comment:', error);
            alert('Error al eliminar el comentario');
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-gray-600">Cargando comentarios...</div>
            </div>
        );
    }

    if (!postId) {
        return <div className="p-8 text-center">No se especificó un ID de publicación.</div>;
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <header className="bg-white shadow-sm">
                <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center">
                    <h1 className="text-2xl font-bold text-gray-900">Comentarios</h1>
                    <button
                        onClick={() => router.back()}
                        className="text-gray-600 hover:text-gray-900"
                    >
                        Volver
                    </button>
                </div>
            </header>

            <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="bg-white rounded-lg shadow p-6 mb-6">
                    <h2 className="text-sm font-medium text-gray-500 uppercase tracking-wider mb-2">Publicación Original</h2>
                    <p className="text-gray-900">{postContent}</p>
                </div>

                <div className="bg-white rounded-lg shadow overflow-hidden">
                    <div className="px-6 py-4 border-b border-gray-200">
                        <h2 className="text-lg font-semibold text-gray-900">Lista de Comentarios</h2>
                    </div>
                    <div className="divide-y divide-gray-200">
                        {comments.length === 0 ? (
                            <div className="px-6 py-8 text-center text-gray-500">
                                No hay comentarios en esta publicación
                            </div>
                        ) : (
                            comments.map((comment) => (
                                <div key={comment.id} className="px-6 py-4 flex justify-between items-start">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="font-bold text-gray-900 text-sm">{comment.userName}</span>
                                            <span className="text-xs text-gray-500">
                                                {new Date(comment.timestamp.seconds * 1000).toLocaleString()}
                                            </span>
                                            {comment.likesCount ? (
                                                <span className="text-xs text-red-500 flex items-center gap-1">
                                                    ❤️ {comment.likesCount}
                                                </span>
                                            ) : null}
                                        </div>
                                        <p className="text-gray-700 text-sm">{comment.content}</p>
                                    </div>
                                    <div className="flex gap-4">
                                        <button
                                            onClick={async () => {
                                                const newContent = prompt('Editar comentario:', comment.content);
                                                if (newContent && newContent !== comment.content) {
                                                    try {
                                                        await updateComment(comment.id!, { content: newContent });
                                                        if (postId) loadComments(postId);
                                                    } catch (error) {
                                                        alert('Error al actualizar');
                                                    }
                                                }
                                            }}
                                            className="text-blue-600 hover:text-blue-900 text-sm font-medium"
                                        >
                                            Editar
                                        </button>
                                        <button
                                            onClick={() => handleDelete(comment.id!)}
                                            className="text-red-600 hover:text-red-900 text-sm font-medium"
                                        >
                                            Eliminar
                                        </button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}

export default function PostCommentsPage() {
    return (
        <Suspense fallback={<div className="min-h-screen flex items-center justify-center">Cargando...</div>}>
            <PostCommentsContent />
        </Suspense>
    );
}
