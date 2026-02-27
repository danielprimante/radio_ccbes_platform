'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { Timestamp } from 'firebase/firestore';
import { auth } from '@/lib/firebase';
import { createPost, getUser } from '@/lib/api';
import AdminGuard from '@/components/AdminGuard';

export default function NewPostPage() {
    const [user, setUser] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [formData, setFormData] = useState({
        content: '',
        imageUrl: '',
        category: 'news' as 'all' | 'trending' | 'news' | 'reflections'
    });
    const router = useRouter();

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
            if (currentUser) {
                try {
                    // Fetch full profile from Firestore to get latest photoUrl
                    const userProfile = await getUser(currentUser.uid);
                    if (userProfile) {
                        setUser({ ...currentUser, ...userProfile });
                    } else {
                        setUser(currentUser);
                    }
                } catch (error) {
                    console.error("Error fetching user profile:", error);
                    setUser(currentUser);
                }
            }
            setLoading(false);
        });

        return () => unsubscribe();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSubmitting(true);

        try {
            const postData: any = {
                userId: user.uid,
                userName: user.name || user.displayName || 'Admin',
                userHandle: user.handle || '@admin',
                content: formData.content,
                likes: 0,
                comments: 0,
                timestamp: Timestamp.now(),
                category: formData.category
            };

            // Only add optional fields if they have values
            // Prioritize Firestore photoUrl over Auth photoURL
            if (user.photoUrl || user.photoURL) {
                postData.userPhotoUrl = user.photoUrl || user.photoURL;
            }
            if (formData.imageUrl) {
                postData.imageUrl = formData.imageUrl;
            }

            await createPost(postData);

            router.push('/admin');
        } catch (error) {
            console.error('Error creating post:', error);
            alert('Error al crear la publicación');
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

    return (
        <AdminGuard>
            <div className="min-h-screen bg-gray-50">
                <header className="bg-white shadow-sm">
                    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
                        <h1 className="text-2xl font-bold text-gray-900">Nueva Publicación</h1>
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
                                {submitting ? 'Publicando...' : 'Publicar'}
                            </button>
                        </div>
                    </form>
                </main>
            </div>
        </AdminGuard>
    );
}
