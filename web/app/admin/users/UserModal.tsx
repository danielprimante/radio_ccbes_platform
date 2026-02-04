import { useState, useEffect } from 'react';
import { User } from '@/lib/api';

interface UserModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (data: any) => Promise<void>;
    user?: User | null;
}

export default function UserModal({ isOpen, onClose, onSubmit, user }: UserModalProps) {
    const [loading, setLoading] = useState(false);
    const [formData, setFormData] = useState({
        email: '',
        password: '',
        name: '',
        handle: '',
        role: 'user',
        bio: ''
    });

    useEffect(() => {
        if (user) {
            setFormData({
                email: user.email || '',
                password: '', // Don't show password
                name: user.name || '',
                handle: user.handle || '',
                role: user.role || 'user',
                bio: user.bio || ''
            });
        } else {
            setFormData({
                email: '',
                password: '',
                name: '',
                handle: '',
                role: 'user',
                bio: ''
            });
        }
    }, [user, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        try {
            await onSubmit(formData);
            onClose();
        } catch (error) {
            console.error(error);
            alert(error instanceof Error ? error.message : 'Error al guardar usuario');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-lg max-w-md w-full p-6">
                <h2 className="text-xl font-bold mb-4">
                    {user ? 'Editar Usuario' : 'Nuevo Usuario'}
                </h2>

                {!user ? (
                    <div className="space-y-4">
                        <div className="bg-amber-50 border border-amber-200 p-4 rounded-lg">
                            <h3 className="font-bold text-amber-800 flex items-center gap-2">
                                <span className="material-symbols-outlined">info</span>
                                Funcionalidad Limitada
                            </h3>
                            <p className="text-sm text-amber-700 mt-2">
                                El sitio está corriendo en modo estático (Serverless). Por seguridad,
                                la creación de nuevas cuentas de acceso debe hacerse desde la consola de Firebase.
                            </p>
                        </div>

                        <div className="bg-gray-50 p-4 rounded-lg border border-gray-200 space-y-3">
                            <p className="text-sm font-semibold">Pasos para crear usuario:</p>
                            <ol className="list-decimal list-inside text-sm text-gray-600 space-y-2">
                                <li>Accede a <a href="https://console.firebase.google.com/" target="_blank" rel="noopener noreferrer" className="text-blue-600 underline">Firebase Console</a></li>
                                <li>Ve a <strong>Authentication</strong> &gt; <strong>Users</strong></li>
                                <li>Haz clic en <strong>Add user</strong></li>
                                <li>Ingresa email y contraseña</li>
                            </ol>
                            <p className="text-xs text-gray-500 mt-2">
                                * Una vez creado, el usuario aparecerá automáticamente en esta lista y podrás editar su perfil o asignarle rol de administrador aquí.
                            </p>
                        </div>

                        <div className="flex justify-end pt-2">
                            <button
                                type="button"
                                onClick={onClose}
                                className="bg-gray-100 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-200 font-medium"
                            >
                                Entendido, cerrar
                            </button>
                        </div>
                    </div>
                ) : (
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Email (Solo lectura)</label>
                            <input
                                type="email"
                                disabled
                                className="mt-1 block w-full rounded-md border-gray-200 bg-gray-50 text-gray-500 shadow-sm border p-2 cursor-not-allowed"
                                value={formData.email}
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Nombre</label>
                            <input
                                type="text"
                                required
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                value={formData.name}
                                onChange={e => setFormData({ ...formData, name: e.target.value })}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Handle (@usuario)</label>
                            <input
                                type="text"
                                required
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                value={formData.handle}
                                onChange={e => setFormData({ ...formData, handle: e.target.value })}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Rol</label>
                            <select
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                value={formData.role}
                                onChange={e => setFormData({ ...formData, role: e.target.value })}
                            >
                                <option value="user">Usuario</option>
                                <option value="admin">Administrador</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Biografía</label>
                            <textarea
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                value={formData.bio}
                                onChange={e => setFormData({ ...formData, bio: e.target.value })}
                                rows={3}
                            />
                        </div>

                        <div className="flex justify-end gap-3 pt-4">
                            <button
                                type="button"
                                onClick={onClose}
                                className="px-4 py-2 text-gray-700 hover:text-gray-900"
                            >
                                Cancelar
                            </button>
                            <button
                                type="submit"
                                disabled={loading}
                                className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 disabled:opacity-50"
                            >
                                {loading ? 'Guardando...' : 'Guardar Cambios'}
                            </button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}
