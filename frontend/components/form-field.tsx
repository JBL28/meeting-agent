import type { InputHTMLAttributes } from 'react';

export function FormField({ label, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className="block text-sm font-medium text-slate-700">
      {label}
      <input
        className="mt-2 w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
        {...props}
      />
    </label>
  );
}
