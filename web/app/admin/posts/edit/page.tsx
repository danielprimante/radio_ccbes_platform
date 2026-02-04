'use client';

import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { Timestamp } from 'firebase/firestore';
import { auth } from '@/lib/firebase';
import { getPost, updatePost, type Post } from '@/lib/api';
import AdminGuard from '@/components/AdminGuard';

function EditPostContent() {
    const searchParams = useSearchParams();
    const postId = searchParams.get('id');
    const [user, setUser] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [post, setPost] = useState<Post | null>(null);
    const [formData, setFormData] = useState({
        content: '',
        imageUrl: '',
        category: 'news' as 'all' | 'trending' | 'news' | 'reflections'
    });
    const router = useRouter();

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
            if (currentUser) {
                setUser(currentUser);
                if (postId) {
                    await loadPost(postId);
                } else {
                    router.push('/admin');
                }
            } else {
                router.push('/login');
            }
            setLoading(false);
        });

        return () => unsubscribe();
    }, [postId]);

    const loadPost = async (id: string) => {
        try {
            const postData = await getPost(id);
            if (postData) {
                setPost(postData);
                setFormData({
                    content: postData.content,
                    imageUrl: postData.imageUrl || '',
                    category: postData.category
                });
            } else {
                alert('Publicación no encontrada');
                router.push('/admin');
            }
        } catch (error) {
            console.error('Error loading post:', error);
            alert('Error al cargar la publicación');
            router.push('/admin');
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!postId) return;

        setSubmitting(true);

        try {
            const updateData: any = {
                content: formData.content,
                category: formData.category
            };

            // Only add optional fields if they have values
            if (formData.imageUrl) {
                updateData.imageUrl = formData.imageUrl;
            }

            await updatePost(postId, updateData);
            router.push('/admin');
        } catch (error) {
            console.error('Error updating post:', error);
            alert('Error al actualizar la publicación');
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-gray-600">Cargando...</div>
            </div>
        );
    }

    if (!post) {
        return null;
    }

    return (
        <AdminGuard>
            <div className="min-h-screen bg-gray-50">
                <header className="bg-white shadow-sm">
                    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
                        <h1 className="text-2xl font-bold text-gray-900">Editar Publicación</h1>
                    </div>
                </header>

                <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                    <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 space-y-6">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Contenido
                            </label>
                            <textarea
                                value={formData.content}
                                onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                                rows={6}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="Escribe el contenido de la publicación..."
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                URL de Imagen (opcional)
                            </label>
                            <input
                                type="url"
                                value={formData.imageUrl}
                                onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                placeholder="https://ejemplo.com/imagen.jpg"
                            />
                            <p className="mt-1 text-xs text-gray-500">
                                Puedes usar servicios gratuitos como Imgur, ImgBB o Cloudinary
                            </p>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Categoría
                            </label>
                            <select
                                value={formData.category}
                                onChange={(e) => setFormData({ ...formData, category: e.target.value as any })}
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                            >
                                <option value="news">Noticias</option>
                                <option value="trending">Tendencias</option>
                                <option value="reflections">Reflexiones</option>
                            </select>
                        </div>

                        <div className="bg-gray-50 p-4 rounded-lg">
                            <div className="text-sm text-gray-600">
                                <p><strong>Autor:</strong> {post.userName}</p>
                                <p><strong>Publicado:</strong> {new Date(post.timestamp.toDate()).toLocaleString('es-ES')}</p>
                                <p><strong>Likes:</strong> {post.likes} | <strong>Comentarios:</strong> {post.comments}</p>
                            </div>
                        </div>

                        <div className="flex gap-4">
                            <button
                                type="button"
                                onClick={() => router.back()}
                                className="flex-1 px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                            >
                                Cancelar
                            </button>
                            <button
                                type="submit"
                                disabled={submitting}
                                className="flex-1 px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {submitting ? 'Guardando...' : 'Guardar Cambios'}
                            </button>
                        </div>
                    </form>
                </main>
            </div>
        </AdminGuard>
    );
}

export default function EditPostPage() {
    return (
        <Suspense fallback={<div className="min-h-screen flex items-center justify-center">Cargando...</div>}>
            <EditPostContent />
        </Suspense>
    );
}
