<script setup lang="ts">
// ORGANISM · ShopMock — ported from the `.shop-mock` markup inlined in
// components/organisms/popup-stage.html.twig. A static, aria-hidden storefront backdrop the
// widget previews sit on top of; carries no props — the old Twig markup never varied it either.
</script>

<template>
    <div class="shop-mock" aria-hidden="true">
        <div class="shop-mock__bar">
            <span class="shop-mock__logo">aureashop</span>
            <span class="shop-mock__nav"></span
            ><span class="shop-mock__nav"></span
            ><span class="shop-mock__nav"></span>
            <span class="shop-mock__cart"></span>
        </div>
        <div class="shop-mock__hero"></div>
        <div class="shop-mock__grid">
            <span></span><span></span><span></span><span></span>
        </div>
    </div>
</template>
